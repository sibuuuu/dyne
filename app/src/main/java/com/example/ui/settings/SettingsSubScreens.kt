package com.example.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ads.LocalAdManager
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.billing.SoundPack
import com.example.data.HapticStrength
import com.example.data.ScanRepository
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.DyneSwitch
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassSheet
import com.example.ui.components.GlassSurface
import com.example.ui.components.HazeState
import com.example.ui.components.LocalTapSound
import com.example.ui.components.ProBadge
import com.example.ui.components.dyneClickable
import com.example.ui.components.proBorder
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldDeep
import com.example.ui.theme.StatusDanger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Authenticates the user with Biometrics or Device PIN/Credential.
 * Falls back gracefully if the device/emulator has no enrolled biometrics or lock screen.
 */
fun authenticateWithBiometricsOrDeviceCredential(
    context: Context,
    title: String,
    subtitle: String,
    allowDeviceCredential: Boolean = true,
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {}
) {
    val activity = context as? FragmentActivity
    val biometricManager = BiometricManager.from(context)
    val authenticators = if (allowDeviceCredential) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
    }

    val canAuth = biometricManager.canAuthenticate(authenticators)
    if (activity != null && canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            }
        )
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
        if (!allowDeviceCredential) {
            promptInfoBuilder.setNegativeButtonText("Cancel")
        }
        runCatching {
            prompt.authenticate(promptInfoBuilder.build())
        }.onFailure {
            onSuccess()
        }
    } else {
        // Emulator or device without lock screen enrolled: succeed directly so user is never locked out
        onSuccess()
    }
}

/**
 * Full-screen Glass App Lock overlay enforced when [appLockEnabled] is true:
 * - Locks on cold launch when [lockOnLaunch] is true
 * - Locks after [timeoutMinutes] (1 / 5 / 15 min) in the background
 */
@Composable
fun DyneAppLockOverlay(
    appLockEnabled: Boolean,
    appLockMethod: String,
    lockOnLaunch: Boolean,
    timeoutMinutes: Int,
    isSplashActive: Boolean
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCheckedInitialLaunch by rememberSaveable { mutableStateOf(false) }
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var backgroundedAtMs by rememberSaveable { mutableLongStateOf(0L) }
    var authErrorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appLockEnabled, lockOnLaunch, isSplashActive) {
        if (!isSplashActive && !hasCheckedInitialLaunch) {
            hasCheckedInitialLaunch = true
            if (appLockEnabled && lockOnLaunch) {
                isLocked = true
            }
        }
        if (!appLockEnabled) {
            isLocked = false
        }
    }

    DisposableEffect(lifecycleOwner, appLockEnabled, timeoutMinutes) {
        val observer = LifecycleEventObserver { _, event ->
            if (!appLockEnabled) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundedAtMs = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    val bgAt = backgroundedAtMs
                    if (bgAt > 0L) {
                        val elapsedMs = System.currentTimeMillis() - bgAt
                        val thresholdMs = timeoutMinutes.coerceIn(1, 60) * 60_000L
                        if (elapsedMs >= thresholdMs) {
                            isLocked = true
                        }
                        backgroundedAtMs = 0L
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (appLockEnabled && isLocked && !isSplashActive) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background.copy(alpha = 0.96f))
                .padding(24.dp)
                .testTag("dyne_app_lock_overlay")
        ) {
            GlassSurface(
                contentPadding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    com.example.DyneLogo(size = 56.dp)
                    Text(
                        text = "Dyne App Lock",
                        style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = when (appLockMethod) {
                            "BIOMETRIC" -> "Authenticate with biometrics to unlock Dyne."
                            "DEVICE_PIN" -> "Authenticate with your device PIN or pattern to unlock Dyne."
                            else -> "Authenticate with biometrics or device PIN to unlock Dyne."
                        },
                        style = type.body,
                        color = theme.textSecondary
                    )
                    if (authErrorText != null) {
                        Text(
                            text = authErrorText ?: "",
                            style = type.caption,
                            color = StatusDanger
                        )
                    }
                    GlassButton(
                        text = "Unlock Dyne",
                        icon = Icons.Default.Fingerprint,
                        accentFill = theme.accent,
                        onClick = {
                            authErrorText = null
                            authenticateWithBiometricsOrDeviceCredential(
                                context = context,
                                title = "Unlock Dyne",
                                subtitle = "Verify your identity to continue",
                                allowDeviceCredential = appLockMethod != "BIOMETRIC",
                                onSuccess = { isLocked = false },
                                onError = { err -> authErrorText = err }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "app_lock_unlock_button"
                    )
                }
            }
        }
    }
}

/**
 * 2. SOUNDS & HAPTICS SUB-SCREEN
 */
@Composable
fun SoundsAndHapticsSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val tapSound = LocalTapSound.current
    val featureGate = LocalFeatureGate.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val ownedPacks by viewModel.ownedSoundPacks.collectAsStateWithLifecycle()
    val packPrices by viewModel.soundPackPrices.collectAsStateWithLifecycle()

    var sliderVolDraft by remember(prefs.tapSoundVolume) {
        mutableFloatStateOf(prefs.tapSoundVolume)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenTopBar(
            title = "Sounds & Haptics",
            subtitle = "ACOUSTIC CLICKS, SCAN TONES & TACTILE ENGINE",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            InsetSettingsGroup(header = "TAP SOUNDS & VOLUME") {
                InsetSettingsSwitchRow(
                    icon = Icons.Default.MusicNote,
                    iconBg = theme.accent,
                    title = "Tap Sounds",
                    subtitle = "Crisp acoustic click on buttons, tiles & tabs (muted in Sound Meter & Silent mode)",
                    query = "",
                    checked = prefs.tapSoundsEnabled,
                    onCheckedChange = viewModel::setTapSoundsEnabled,
                    testTag = "settings_switch_tap_sounds"
                )

                InsetDivider()

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                bgColor = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "Tap Volume",
                                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                color = theme.textPrimary
                            )
                        }
                        Text(
                            text = "${(sliderVolDraft * 100).roundToInt()}%",
                            style = type.gaugeSmall,
                            color = theme.accent
                        )
                    }

                    Slider(
                        value = sliderVolDraft,
                        onValueChange = {
                            sliderVolDraft = it
                            viewModel.setTapSoundVolume(it)
                        },
                        onValueChangeFinished = {
                            tapSound?.playTap(
                                view = view,
                                forcePreview = true,
                                customVolume = sliderVolDraft
                            )
                        },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.accent,
                            activeTrackColor = theme.accent,
                            inactiveTrackColor = theme.elevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_slider_tap_volume")
                    )
                }

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.PlayArrow,
                    iconBg = Color(0xFF34D399),
                    title = "Play Preview",
                    query = "",
                    valueText = "Tap to test",
                    onClick = {
                        tapSound?.playTap(
                            view = view,
                            forcePreview = true,
                            customVolume = prefs.tapSoundVolume
                        )
                    },
                    testTag = "settings_row_play_preview"
                )

                InsetDivider()

                InsetSettingsSwitchRow(
                    icon = Icons.Default.QrCodeScanner,
                    iconBg = theme.accent,
                    title = "Scan Beep",
                    subtitle = "Soft confirmation tone on QR & barcode detection",
                    query = "",
                    checked = prefs.scanBeepEnabled,
                    onCheckedChange = viewModel::setScanBeepEnabled
                )
            }

            InsetSettingsGroup(header = "HAPTIC FEEDBACK") {
                InsetSettingsSwitchRow(
                    icon = Icons.Default.Vibration,
                    iconBg = Color(0xFFFB7185),
                    title = "Haptics",
                    subtitle = "Tactile feedback on taps, switches & 0° level alignment",
                    query = "",
                    checked = prefs.hapticsEnabled,
                    onCheckedChange = viewModel::setHapticsEnabled
                )

                InsetDivider()

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Haptic Strength",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    DyneSegmentedControl(
                        segments = HapticStrength.entries.map { it.label },
                        selectedIndex = HapticStrength.entries.indexOf(prefs.hapticStrength).coerceAtLeast(0),
                        onSegmentSelected = { idx ->
                            viewModel.setHapticStrength(HapticStrength.entries[idx])
                        }
                    )
                }
            }

            InsetSettingsGroup(header = "ACOUSTIC SOUND PACKS") {
                SoundPack.entries.forEachIndexed { idx, pack ->
                    if (idx > 0) InsetDivider()
                    val isUnlocked = isPro ||
                        ownedPacks.contains(pack.productId) ||
                        prefs.ownedSoundPacksSet.contains(pack.productId) ||
                        prefs.isFeatureUnlockedTemporarily("pack:${pack.productId}")
                    val priceLabel = packPrices[pack.productId] ?: "${pack.coinCost1h} coins / 1h"
                    InsetSettingsRow(
                        icon = Icons.Default.MusicNote,
                        iconBg = if (isUnlocked) Color(0xFF34D399) else ProGold,
                        title = pack.title,
                        query = "",
                        valueText = if (isUnlocked) "Active" else priceLabel,
                        onClick = {
                            if (isUnlocked) {
                                viewModel.showStatusMessage("${pack.title} is unlocked & active")
                            } else if (featureGate != null) {
                                featureGate.requirePack(
                                    pack = pack,
                                    onUnlocked = {
                                        viewModel.showStatusMessage("Unlocked ${pack.title}")
                                    }
                                )
                            } else if (activity != null) {
                                viewModel.launchSoundPackPurchase(activity, pack)
                            } else {
                                onOpenPaywall()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

/**
 * 4. LOCATION SUB-SCREEN
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onBack: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    var latInput by remember(prefs.manualLat) {
        mutableStateOf(String.format(Locale.US, "%.4f", prefs.manualLat))
    }
    var lngInput by remember(prefs.manualLng) {
        mutableStateOf(String.format(Locale.US, "%.4f", prefs.manualLng))
    }
    var labelInput by remember(prefs.locationName) {
        mutableStateOf(prefs.locationName)
    }

    val presetCities = remember {
        listOf(
            Triple("San Francisco, US", 37.7749, -122.4194),
            Triple("New York, US", 40.7128, -74.0060),
            Triple("London, UK", 51.5074, -0.1278),
            Triple("Tokyo, JP", 35.6762, 139.6503),
            Triple("Sydney, AU", -33.8688, 151.2093),
            Triple("Zurich, CH", 47.3769, 8.5417),
            Triple("Anchorage, US", 61.2181, -149.9003),
            Triple("Reykjavik, IS", 64.1466, -21.9426)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenTopBar(
            title = "Location",
            subtitle = "EPHEMERIS, SOLUNAR & WEATHER COORDINATES",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            InsetSettingsGroup(header = "LOCATION SOURCE") {
                InsetSettingsSwitchRow(
                    icon = Icons.Default.MyLocation,
                    iconBg = Color(0xFF38BDF8),
                    title = "Use Manual Coordinates",
                    subtitle = "Override device GPS with fixed coordinates for Solunar, Sun & Moon, and Weather",
                    query = "",
                    checked = prefs.useManualLocation,
                    onCheckedChange = { useManual ->
                        val lat = latInput.toDoubleOrNull() ?: prefs.manualLat
                        val lng = lngInput.toDoubleOrNull() ?: prefs.manualLng
                        viewModel.setManualLocation(lat, lng, useManual, labelInput.ifBlank { "Manual Location" })
                    },
                    testTag = "settings_switch_manual_location"
                )
            }

            InsetSettingsGroup(header = "MANUAL COORDINATES & LABEL") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Location Label",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    BasicTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        singleLine = true,
                        textStyle = type.body.copy(color = theme.textPrimary),
                        cursorBrush = SolidColor(theme.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.elevated)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Latitude (-90 to 90)",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.textSecondary
                            )
                            BasicTextField(
                                value = latInput,
                                onValueChange = { latInput = it },
                                singleLine = true,
                                textStyle = type.body.copy(color = theme.textPrimary),
                                cursorBrush = SolidColor(theme.accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(theme.elevated)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Longitude (-180 to 180)",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.textSecondary
                            )
                            BasicTextField(
                                value = lngInput,
                                onValueChange = { lngInput = it },
                                singleLine = true,
                                textStyle = type.body.copy(color = theme.textPrimary),
                                cursorBrush = SolidColor(theme.accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(theme.elevated)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }

                    GlassButton(
                        text = "Save Manual Location",
                        icon = Icons.Default.Check,
                        accentFill = theme.accent,
                        onClick = {
                            val lat = (latInput.toDoubleOrNull() ?: prefs.manualLat).coerceIn(-90.0, 90.0)
                            val lng = (lngInput.toDoubleOrNull() ?: prefs.manualLng).coerceIn(-180.0, 180.0)
                            viewModel.setManualLocation(
                                lat = lat,
                                lng = lng,
                                useManual = true,
                                label = labelInput.ifBlank { "Custom Coordinates" }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "save_manual_location_button"
                    )
                }
            }

            InsetSettingsGroup(header = "QUICK REFERENCE CITIES") {
                presetCities.forEachIndexed { idx, (cityName, lat, lng) ->
                    if (idx > 0) InsetDivider()
                    val isSelected = prefs.useManualLocation && prefs.locationName == cityName
                    InsetSettingsRow(
                        icon = Icons.Default.Place,
                        iconBg = if (isSelected) theme.accent else Color(0xFF60A5FA),
                        title = cityName,
                        query = "",
                        valueText = String.format(Locale.US, "%.2f°, %.2f°", lat, lng),
                        showChevron = false,
                        onClick = {
                            latInput = String.format(Locale.US, "%.4f", lat)
                            lngInput = String.format(Locale.US, "%.4f", lng)
                            labelInput = cityName
                            viewModel.setManualLocation(lat, lng, true, cityName)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

/**
 * 6. REWARDS & COINS SUB-SCREEN
 */
@Composable
fun RewardsAndCoinsSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onOpenRewards: () -> Unit,
    onOpenPaywall: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = LocalAdManager.current
    val featureGate = LocalFeatureGate.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()

    val active1hUnlocks = remember(
        prefs.featureUnlocksCsv,
        prefs.rewardedToolUnlocksCsv,
        prefs.rewardedThemeUnlocksCsv
    ) {
        featureGate?.getActive1hUnlockDescriptions(System.currentTimeMillis()) ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenTopBar(
            title = "Rewards & Coins",
            subtitle = "COIN BALANCE, STREAK & 1-HOUR UNLOCKS",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val spotlightTool = remember(prefs.spotlightToolId, prefs.spotlightDailyDate) {
                val today = java.time.LocalDate.now().toString()
                val allPro = com.example.domain.ToolRegistry.tools.value.filter { it.isPro && it.isEnabled }
                if (prefs.spotlightDailyDate == today && prefs.spotlightToolId.isNotBlank()) {
                    allPro.firstOrNull { it.id == prefs.spotlightToolId } ?: allPro.firstOrNull()
                } else {
                    allPro.firstOrNull()
                }
            }

            InsetSettingsGroup(header = "WALLET & STREAK") {
                InsetSettingsRow(
                    icon = Icons.Default.MonetizationOn,
                    iconBg = ProGold,
                    title = "Coin Balance",
                    query = "",
                    valueText = "${prefs.coinBalance} coins",
                    onClick = onOpenRewards,
                    testTag = "settings_rewards_balance_row"
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.StarRate,
                    iconBg = Color(0xFF34D399),
                    title = "Daily Check-In Streak",
                    query = "",
                    valueText = "${prefs.checkInStreak} / 7 days",
                    onClick = onOpenRewards
                )

                if (spotlightTool != null && !isPro) {
                    InsetDivider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("settings_rewards_daily_spotlight_row")
                    ) {
                        Text(
                            text = "Free for 1h today: ${spotlightTool.name}",
                            style = type.body.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.accent.copy(alpha = 0.16f))
                                .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .dyneClickable(onClick = onOpenRewards)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Try 1h",
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                color = theme.accent,
                                maxLines = 1
                            )
                        }
                    }
                }

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.PlayArrow,
                    iconBg = Color(0xFF38BDF8),
                    title = "Watch Ad for +10 Coins",
                    query = "",
                    valueText = "${prefs.adCoinDailyCount}/12 today",
                    onClick = {
                        if (adManager != null && activity != null) {
                            adManager.showRewardedAd(
                                activity = activity,
                                rewardDescription = "+10 Dyne Coins",
                                onAdUnavailable = { msg ->
                                    viewModel.showStatusMessage(msg)
                                },
                                onRewardEarned = {
                                    viewModel.claimRewardedAdCoins()
                                }
                            )
                        } else {
                            onOpenRewards()
                        }
                    }
                )
            }

            InsetSettingsGroup(header = "ACTIVE 1-HOUR TEMPORARY UNLOCKS (${active1hUnlocks.size})") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (isPro) {
                        Text(
                            text = "Dyne Premium is active — all tools, themes, and features are permanently unlocked.",
                            style = type.body,
                            color = ProGold
                        )
                    } else if (active1hUnlocks.isEmpty()) {
                        Text(
                            text = "No temporary 1-hour unlocks active right now. Use coins or watch a short rewarded ad on any Pro tool or theme to unlock it for 1 hour.",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    } else {
                        active1hUnlocks.forEach { desc ->
                            Text(
                                text = "• $desc",
                                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                color = ProGold
                            )
                        }
                    }
                }
            }

            GlassButton(
                text = "Open Full Rewards & Coin Ledger",
                icon = Icons.Default.MonetizationOn,
                accentFill = theme.accent,
                onClick = onOpenRewards,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open_full_rewards_button"
            )

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

/**
 * 7. NOTIFICATIONS SUB-SCREEN:
 * Solunar alerts, expiry reminders, daily bonus; each with a switch and quiet hours.
 */
@Composable
fun NotificationsSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onBack: () -> Unit
) {
    NotificationCenterSubScreenContent(
        viewModel = viewModel,
        hazeState = hazeState,
        onBack = onBack
    )
}

@Composable
private fun NotificationCategoryGroup(
    header: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    enabled: Boolean,
    quietHoursEnabled: Boolean,
    quietHoursValue: String,
    quietHourPresets: List<String>,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onSelectQuietHours: (String) -> Unit,
    switchTestTag: String
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    InsetSettingsGroup(header = header) {
        InsetSettingsSwitchRow(
            icon = icon,
            iconBg = iconBg,
            title = title,
            subtitle = subtitle,
            query = "",
            checked = enabled,
            onCheckedChange = onToggleEnabled,
            testTag = switchTestTag
        )

        InsetDivider()

        InsetSettingsSwitchRow(
            icon = Icons.Default.Notifications,
            iconBg = Color(0xFF818CF8),
            title = "Quiet Hours",
            subtitle = if (quietHoursEnabled) "Mute alerts during $quietHoursValue" else "Alerts allowed at any hour",
            query = "",
            checked = quietHoursEnabled,
            onCheckedChange = onToggleQuietHours
        )

        if (quietHoursEnabled) {
            InsetDivider()
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
                    Text(
                        text = "Quiet Hours Window",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    Text(
                        text = quietHoursValue,
                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.accent
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(quietHourPresets) { windowStr ->
                        val isSelected = quietHoursValue == windowStr
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) theme.accent else theme.elevated)
                                .border(
                                    1.dp,
                                    if (isSelected) theme.accent else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .dyneClickable(onClick = { onSelectQuietHours(windowStr) })
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = windowStr,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 8. PRIVACY & SECURITY SUB-SCREEN:
 * - App Lock with biometrics or device PIN (Premium; lock on launch and after 1/5/15 minutes in the background)
 * - Hide app preview in recents (FLAG_SECURE, Premium)
 * - Per-feature locks for Documents and Notes (Premium)
 * - Ad privacy choices (UMP), permission shortcuts and "Clear all local data" (Free)
 */
@Composable
fun PrivacyAndSecuritySubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit,
    onRequestClearAllData: () -> Unit,
    onRequestAdPrivacySheet: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = LocalAdManager.current
    val featureGate = LocalFeatureGate.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val isSecurityUnlocked = isPro ||
        (featureGate?.isUnlocked(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) == true)

    fun requirePremiumSecurity(action: () -> Unit) {
        if (isSecurityUnlocked) {
            action()
        } else if (featureGate != null) {
            featureGate.require(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) {
                action()
            }
        } else {
            onOpenPaywall()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenTopBar(
            title = "Privacy & Security",
            subtitle = "APP LOCK, FLAG_SECURE, BIOMETRICS & UMP",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. APP LOCK (PREMIUM)
            InsetSettingsGroup(header = "APP LOCK (PREMIUM)") {
                InsetSettingsSwitchRow(
                    icon = Icons.Default.Lock,
                    iconBg = ProGoldDeep,
                    title = "App Lock (Biometrics or Device PIN)",
                    subtitle = "Protect Dyne with fingerprint, face, or device PIN/pattern",
                    query = "",
                    checked = prefs.appLockEnabled,
                    isProSetting = true,
                    testTag = "settings_switch_app_lock",
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            requirePremiumSecurity {
                                authenticateWithBiometricsOrDeviceCredential(
                                    context = context,
                                    title = "Enable Dyne App Lock",
                                    subtitle = "Confirm biometrics or device PIN",
                                    allowDeviceCredential = true,
                                    onSuccess = { viewModel.setAppLockEnabled(true) },
                                    onError = { err -> viewModel.showStatusMessage(err) }
                                )
                            }
                        } else {
                            viewModel.setAppLockEnabled(false)
                        }
                    }
                )

                InsetDivider()

                // Auth Method (Biometrics + PIN / Biometrics Only / Device PIN)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Authentication Method",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    val methodLabels = listOf("Biometrics or PIN", "Biometrics", "Device PIN")
                    val methodCodes = listOf("BIOMETRIC_OR_PIN", "BIOMETRIC", "DEVICE_PIN")
                    val selectedMethodIdx = methodCodes.indexOf(prefs.appLockMethod).coerceAtLeast(0)
                    DyneSegmentedControl(
                        segments = methodLabels,
                        selectedIndex = selectedMethodIdx,
                        onSegmentSelected = { idx ->
                            requirePremiumSecurity {
                                viewModel.setAppLockMethod(methodCodes[idx])
                            }
                        }
                    )
                }

                InsetDivider()

                InsetSettingsSwitchRow(
                    icon = Icons.Default.Fingerprint,
                    iconBg = Color(0xFF38BDF8),
                    title = "Lock on Launch",
                    subtitle = "Require authentication whenever Dyne starts",
                    query = "",
                    checked = prefs.appLockOnLaunch,
                    isProSetting = true,
                    onCheckedChange = { enabled ->
                        requirePremiumSecurity {
                            viewModel.setAppLockOnLaunch(enabled)
                        }
                    }
                )

                InsetDivider()

                // Lock after 1 / 5 / 15 minutes in the background
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
                        Text(
                            text = "Lock After Background Timeout",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textSecondary
                        )
                        Text(
                            text = "${prefs.appLockTimeoutMinutes} min",
                            style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                            color = theme.accent
                        )
                    }
                    val timeouts = listOf(1, 5, 15)
                    DyneSegmentedControl(
                        segments = timeouts.map { "$it min" },
                        selectedIndex = timeouts.indexOf(prefs.appLockTimeoutMinutes).let { if (it == -1) 1 else it },
                        onSegmentSelected = { idx ->
                            requirePremiumSecurity {
                                viewModel.setAppLockTimeoutMinutes(timeouts[idx])
                            }
                        }
                    )
                }
            }

            // 2. RECENTS PRIVACY & PER-FEATURE LOCKS (PREMIUM)
            InsetSettingsGroup(header = "RECENTS PREVIEW & PER-FEATURE LOCKS (PREMIUM)") {
                InsetSettingsSwitchRow(
                    icon = Icons.Default.VisibilityOff,
                    iconBg = ProGoldDeep,
                    title = "Hide App Preview in Recents",
                    subtitle = "Apply FLAG_SECURE to blank Dyne in Android app switcher & block screenshots",
                    query = "",
                    checked = prefs.hideAppPreviewInRecents,
                    isProSetting = true,
                    testTag = "settings_switch_flag_secure",
                    onCheckedChange = { hide ->
                        if (hide) {
                            requirePremiumSecurity {
                                viewModel.setHideAppPreviewInRecents(true)
                            }
                        } else {
                            viewModel.setHideAppPreviewInRecents(false)
                        }
                    }
                )

                InsetDivider()

                InsetSettingsSwitchRow(
                    icon = Icons.Default.Fingerprint,
                    iconBg = ProGoldDeep,
                    title = "Lock Documents with Biometrics",
                    subtitle = "Require fingerprint / face authentication to open scanned PDFs",
                    query = "",
                    checked = prefs.lockDocumentsWithBiometrics,
                    isProSetting = true,
                    testTag = "settings_switch_lock_documents",
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            requirePremiumSecurity {
                                viewModel.setLockDocumentsWithBiometrics(true)
                            }
                        } else {
                            viewModel.setLockDocumentsWithBiometrics(false)
                        }
                    }
                )

                InsetDivider()

                InsetSettingsSwitchRow(
                    icon = Icons.Default.Description,
                    iconBg = ProGoldDeep,
                    title = "Lock Notes & Sketches with Biometrics",
                    subtitle = "Require fingerprint / face authentication to open field notes & drawings",
                    query = "",
                    checked = prefs.lockNotesWithBiometrics,
                    isProSetting = true,
                    testTag = "settings_switch_lock_notes",
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            requirePremiumSecurity {
                                viewModel.setLockNotesWithBiometrics(true)
                            }
                        } else {
                            viewModel.setLockNotesWithBiometrics(false)
                        }
                    }
                )
            }

            // 3. FREE PRIVACY CONTROLS (UMP, PERMISSIONS, CLEAR ALL LOCAL DATA)
            InsetSettingsGroup(header = "PRIVACY & PERMISSIONS (FREE)") {
                InsetSettingsRow(
                    icon = Icons.Default.PrivacyTip,
                    iconBg = Color(0xFFA78BFA),
                    title = "Ad Privacy Choices (UMP)",
                    query = "",
                    valueText = if (isPro) "No Ads (Pro)" else "UMP Consent",
                    onClick = {
                        if (activity != null && adManager != null) {
                            adManager.openPrivacyOptionsForm(activity) { msg ->
                                viewModel.showStatusMessage(msg)
                                onRequestAdPrivacySheet()
                            }
                        } else {
                            onRequestAdPrivacySheet()
                        }
                    },
                    testTag = "settings_row_ad_privacy"
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Security,
                    iconBg = Color(0xFF38BDF8),
                    title = "Permission Shortcuts",
                    query = "",
                    valueText = "System Settings",
                    onClick = {
                        runCatching {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    },
                    testTag = "settings_row_permission_shortcuts"
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Description,
                    iconBg = Color(0xFF818CF8),
                    title = "Privacy Policy",
                    query = "",
                    valueText = com.example.legal.LegalConfig.PRIVACY_POLICY_URL,
                    onClick = onOpenPrivacyPolicy,
                    testTag = "privacy_security_policy_row"
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.DeleteForever,
                    iconBg = StatusDanger,
                    title = "Clear All Local Data",
                    query = "",
                    valueText = "Reset Vault",
                    titleColor = StatusDanger,
                    onClick = onRequestClearAllData,
                    testTag = "settings_row_clear_data"
                )
            }

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

/**
 * 9. DATA & BACKUP SUB-SCREEN:
 * - Premium: export everything (documents, notes, sketches, scans, settings, coins) to one ZIP
 *   through the system file picker and restore from it, with progress and a version check.
 * - Free users can back up and restore settings only.
 * - Show "Storage used by Dyne" per feature and a Clear cache button.
 */
@Composable
fun DataAndBackupSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val featureGate = LocalFeatureGate.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val storageBreakdown by viewModel.storageBreakdown.collectAsStateWithLifecycle()
    val temporaryCacheBytes by viewModel.temporaryCacheSizeBytes.collectAsStateWithLifecycle()
    val backupProgress by viewModel.backupProgress.collectAsStateWithLifecycle()
    val backupStatusText by viewModel.backupStatusText.collectAsStateWithLifecycle()

    val isFullBackupUnlocked = isPro ||
        (featureGate?.isUnlocked(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) == true)

    var pendingExportIsFull by rememberSaveable { mutableStateOf(false) }
    var pendingRestoreIsFull by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshTemporaryCacheSize(context)
    }

    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackupToUri(
                context = context,
                uri = uri,
                isFullBackup = pendingExportIsFull
            )
        }
    }

    val openZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreBackupFromUri(
                context = context,
                uri = uri,
                allowFullRestore = pendingRestoreIsFull
            )
        }
    }

    fun defaultBackupFileName(full: Boolean): String {
        val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return if (full) "Dyne_Full_Backup_$ts.zip" else "Dyne_Settings_Backup_$ts.zip"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenTopBar(
            title = "Data & Backup",
            subtitle = "ZIP VAULT ARCHIVE, SETTINGS BACKUP & STORAGE",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Progress & Version Check Status Card
            if (backupProgress != null || !backupStatusText.isNullOrBlank()) {
                GlassSurface(
                    contentPadding = PaddingValues(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (backupProgress != null) "Processing Backup Archive..." else "Backup & Version Check",
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                            Text(
                                text = "Schema v${ScanRepository.BACKUP_SCHEMA_VERSION} • App ${BuildConfig.VERSION_NAME}",
                                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = theme.accent
                            )
                        }
                        if (backupProgress != null) {
                            LinearProgressIndicator(
                                progress = { (backupProgress ?: 0f).coerceIn(0f, 1f) },
                                color = theme.accent,
                                trackColor = theme.elevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                        if (!backupStatusText.isNullOrBlank()) {
                            Text(
                                text = backupStatusText ?: "",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                    }
                }
            }

            // 1. PREMIUM FULL ZIP BACKUP & RESTORE
            InsetSettingsGroup(header = "FULL VAULT ZIP BACKUP (PREMIUM)") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoundedSquareIconBadge(
                            icon = Icons.Default.WorkspacePremium,
                            bgColor = ProGold
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Complete Vault & Coin Archive",
                                    style = type.body.copy(fontWeight = FontWeight.Bold),
                                    color = theme.textPrimary
                                )
                                ProBadge()
                            }
                            Text(
                                text = "Exports all documents (PDFs & JPEGs), notes, sketches, QR/barcode scans, settings, and coins into one verified ZIP file.",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassButton(
                            text = "Export Everything (ZIP)",
                            icon = Icons.Default.Share,
                            accentFill = ProGold,
                            textColor = BentoTextBlack,
                            onClick = {
                                if (isFullBackupUnlocked) {
                                    pendingExportIsFull = true
                                    createZipLauncher.launch(defaultBackupFileName(full = true))
                                } else if (featureGate != null) {
                                    featureGate.require(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) {
                                        pendingExportIsFull = true
                                        createZipLauncher.launch(defaultBackupFileName(full = true))
                                    }
                                } else {
                                    onOpenPaywall()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "export_full_backup_zip_button"
                        )

                        GlassButton(
                            text = "Restore Full ZIP",
                            icon = Icons.Default.Restore,
                            onClick = {
                                if (isFullBackupUnlocked) {
                                    pendingRestoreIsFull = true
                                    openZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                } else if (featureGate != null) {
                                    featureGate.require(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) {
                                        pendingRestoreIsFull = true
                                        openZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                    }
                                } else {
                                    onOpenPaywall()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "restore_full_backup_zip_button"
                        )
                    }
                }
            }

            // 2. FREE SETTINGS-ONLY BACKUP & RESTORE
            InsetSettingsGroup(header = "SETTINGS BACKUP & RESTORE (FREE)") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Free users can export and restore all app settings, themes, home layouts, units, and notification preferences to a ZIP archive.",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassButton(
                            text = "Back Up Settings",
                            icon = Icons.Default.FolderOpen,
                            accentFill = theme.accent,
                            onClick = {
                                pendingExportIsFull = false
                                createZipLauncher.launch(defaultBackupFileName(full = false))
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "export_settings_only_zip_button"
                        )
                        GlassButton(
                            text = "Restore Settings",
                            icon = Icons.Default.Restore,
                            onClick = {
                                pendingRestoreIsFull = false
                                openZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "restore_settings_only_zip_button"
                        )
                    }
                }
            }

            // 3. STORAGE USED BY DYNE PER FEATURE + CLEAR CACHE BUTTON
            InsetSettingsGroup(header = "STORAGE USED BY DYNE (TOTAL: ${ScanRepository.formatFileSize(storageBreakdown.totalBytes)})") {
                InsetSettingsRow(
                    icon = Icons.Default.Description,
                    iconBg = Color(0xFF38BDF8),
                    title = "Documents & Scanned PDFs",
                    query = "",
                    valueText = ScanRepository.formatFileSize(storageBreakdown.documentsBytes),
                    showChevron = false,
                    onClick = {}
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.FolderOpen,
                    iconBg = Color(0xFF34D399),
                    title = "Notes, Sketches & Attached Images",
                    query = "",
                    valueText = ScanRepository.formatFileSize(storageBreakdown.notesAndSketchesBytes),
                    showChevron = false,
                    onClick = {}
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.QrCodeScanner,
                    iconBg = Color(0xFFA78BFA),
                    title = "Scans & Local Vault Database",
                    query = "",
                    valueText = ScanRepository.formatFileSize(storageBreakdown.scansAndDatabaseBytes),
                    showChevron = false,
                    onClick = {}
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Build,
                    iconBg = Color(0xFF60A5FA),
                    title = "Settings & Offline Caches",
                    query = "",
                    valueText = ScanRepository.formatFileSize(storageBreakdown.settingsAndPrefsBytes),
                    showChevron = false,
                    onClick = {}
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Restore,
                    iconBg = Color(0xFFFF2D55),
                    title = "Temporary Processing Cache",
                    query = "",
                    valueText = ScanRepository.formatFileSize(temporaryCacheBytes),
                    showChevron = false,
                    onClick = { viewModel.cleanTemporaryCache(context) }
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Description,
                    iconBg = Color(0xFF38BDF8),
                    title = "Clear Media Studio Recent Files",
                    query = "",
                    valueText = "Clear List",
                    onClick = { viewModel.clearMediaRecentFilesHistory() },
                    testTag = "settings_row_clear_media_recents"
                )
            }

            GlassButton(
                text = "Clear Cache (${ScanRepository.formatFileSize(temporaryCacheBytes)})",
                icon = Icons.Default.DeleteForever,
                accentFill = theme.accent,
                onClick = { viewModel.cleanTemporaryCache(context) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "settings_row_cache_cleaner"
            )

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

/**
 * 10. SUPPORT & ABOUT SUB-SCREEN:
 * "Dyne", version, "Built by Sibora", Privacy Policy and Terms (URL placeholders), Rate and Share.
 */
@Composable
fun SupportAndAboutSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onOpenModalSheet: (String) -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var versionTapCounter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenTopBar(
            title = "Support & About",
            subtitle = "DYNE IDENTITY, LEGAL & FEEDBACK",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            InsetSettingsGroup(header = "ABOUT DYNE") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    com.example.DyneLogo(size = 96.dp)
                    Column {
                        Text(
                            text = "Dyne",
                            style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME} • Built by Sibora",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                }

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Explore,
                    iconBg = theme.accent,
                    title = "Dyne",
                    query = "",
                    valueText = "Precision Utility Suite",
                    showChevron = false,
                    onClick = {}
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Code,
                    iconBg = Color(0xFF38BDF8),
                    title = "Version ${BuildConfig.VERSION_NAME}",
                    query = "",
                    valueText = "Build ${BuildConfig.VERSION_CODE}",
                    showChevron = BuildConfig.DEBUG && versionTapCounter >= 4,
                    testTag = "about_subscreen_version_row",
                    onClick = {
                        if (BuildConfig.DEBUG) {
                            versionTapCounter += 1
                            if (versionTapCounter >= 7) {
                                versionTapCounter = 0
                                onOpenDeveloperOptions()
                            } else if (versionTapCounter >= 4) {
                                val rem = 7 - versionTapCounter
                                viewModel.showStatusMessage("$rem more tap${if (rem == 1) "" else "s"} to open Developer options")
                            }
                        }
                    }
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Build,
                    iconBg = Color(0xFF34D399),
                    title = "Built by Sibora",
                    query = "",
                    valueText = "Offline-First Architecture",
                    showChevron = false,
                    onClick = {}
                )
            }

            InsetSettingsGroup(header = "SUPPORT & FEEDBACK") {
                InsetSettingsRow(
                    icon = Icons.Default.StarRate,
                    iconBg = theme.accent,
                    title = "Rate Dyne",
                    query = "",
                    valueText = "Google Play",
                    onClick = {
                        runCatching {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    testTag = "support_rate_dyne_row"
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Share,
                    iconBg = Color(0xFF38BDF8),
                    title = "Share Dyne",
                    query = "",
                    valueText = "Send Link",
                    onClick = {
                        runCatching {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                this.type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Dyne — Precision Instruments & Offline Vault")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out Dyne — precision offline tools, Media Studio, Solunar, Sun & Moon, Weather, and Document Vault: https://play.google.com/store/apps/details?id=${context.packageName}"
                                )
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Dyne"))
                        }
                    },
                    testTag = "support_share_dyne_row"
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Email,
                    iconBg = Color(0xFF34D399),
                    title = "Contact Support",
                    query = "",
                    valueText = com.example.legal.LegalConfig.CONTACT_EMAIL,
                    onClick = {
                        runCatching {
                            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${com.example.legal.LegalConfig.CONTACT_EMAIL}?subject=Dyne%20Feedback")
                            }
                            context.startActivity(mailIntent)
                        }.onFailure {
                            onOpenModalSheet("contact")
                        }
                    }
                )
            }

            InsetSettingsGroup(header = "LEGAL & POLICIES") {
                InsetSettingsRow(
                    icon = Icons.Default.Security,
                    iconBg = Color(0xFF818CF8),
                    title = "Privacy Policy",
                    query = "",
                    valueText = com.example.legal.LegalConfig.PRIVACY_POLICY_URL,
                    onClick = { onOpenModalSheet("privacy") },
                    testTag = "support_privacy_policy_row"
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Description,
                    iconBg = Color(0xFFF472B6),
                    title = "Terms of Use",
                    query = "",
                    valueText = "Google Play Terms",
                    onClick = { onOpenModalSheet("terms") },
                    testTag = "support_terms_row"
                )
            }

            DyneQuietCreditBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            )

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

@Composable
internal fun SubScreenTopBar(
    title: String,
    subtitle: String,
    hazeState: HazeState?,
    onBack: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Settings",
            hazeState = hazeState,
            onClick = onBack,
            testTag = "settings_subscreen_back"
        )
        Column {
            Text(text = subtitle, style = type.caption, color = theme.textSecondary)
            Text(text = title, style = type.largeTitle, color = theme.textPrimary)
        }
    }
}

@Composable
internal fun InsetSettingsGroup(
    header: String,
    content: @Composable () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (header.isNotBlank()) {
            Text(
                text = header.uppercase(),
                style = type.section,
                color = theme.accent,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(theme.surface)
                .border(
                    width = 1.dp,
                    color = if (theme.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            content()
        }
    }
}

@Composable
internal fun InsetDivider() {
    val theme = LocalDyneTheme.current
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        thickness = 0.7.dp,
        color = if (theme.isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)
    )
}

@Composable
internal fun RoundedSquareIconBadge(
    icon: ImageVector,
    bgColor: Color
) {
    val useDarkIcon = bgColor == Color(0xFFCCFF00) || bgColor == Color(0xFFC6F27A) || bgColor == ProGold || bgColor == Color(0xFF4ADE80)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (useDarkIcon) BentoTextBlack else Color.White,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
internal fun InsetSettingsRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    query: String,
    valueText: String = "",
    titleColor: Color? = null,
    showChevron: Boolean = true,
    testTag: String? = null,
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
            Text(
                text = highlightMatch(title, query, theme.accent),
                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor ?: theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (valueText.isNotBlank()) {
                Text(
                    text = highlightMatch(valueText, query, theme.accent),
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = theme.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun InsetSettingsSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    query: String,
    checked: Boolean,
    isProSetting: Boolean = false,
    testTag: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .let { base -> if (isProSetting) base.proBorder(RoundedCornerShape(16.dp)) else base }
            .dyneClickable(
                targetScale = 0.98f,
                testTag = testTag,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            RoundedSquareIconBadge(icon = icon, bgColor = iconBg)
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = highlightMatch(title, query, theme.accent),
                        style = type.cardTitle,
                        color = theme.textPrimary
                    )
                    if (isProSetting) {
                        ProBadge()
                    }
                }
                Text(
                    text = highlightMatch(subtitle, query, theme.accent),
                    style = type.subtitle,
                    color = theme.textSecondary
                )
            }
        }

        DyneSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

internal fun highlightMatch(
    text: String,
    query: String,
    highlightColor: Color
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val startIdx = text.indexOf(query, ignoreCase = true)
    if (startIdx < 0) return AnnotatedString(text)
    val endIdx = (startIdx + query.length).coerceAtMost(text.length)
    return buildAnnotatedString {
        append(text)
        addStyle(
            style = SpanStyle(
                color = BentoTextBlack,
                background = highlightColor,
                fontWeight = FontWeight.ExtraBold
            ),
            start = startIdx,
            end = endIdx
        )
    }
}
