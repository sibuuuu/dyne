package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.billing.PurchaseManager
import com.example.data.HapticStrength
import com.example.data.PreferencesRepository
import com.example.data.ScanRepository
import com.example.data.ThemeMode
import com.example.data.UnitSystem
import com.example.data.UserPreferences
import com.example.domain.HomeNeedPreset
import com.example.domain.ToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OutdoorsSegmentConfig(
    val id: String,
    val title: String,
    val enabled: Boolean
)

class SettingsViewModel(
    val preferencesRepository: PreferencesRepository,
    private val purchaseManager: PurchaseManager,
    private val scanRepository: ScanRepository? = null
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val isPro: StateFlow<Boolean> = purchaseManager.isPro
    val proSource: StateFlow<String> = purchaseManager.proSource
    val isRemoveAds: StateFlow<Boolean> = purchaseManager.isRemoveAds
    val removeAdsPrice: StateFlow<String> = purchaseManager.removeAdsPrice
    val ownedSoundPacks: StateFlow<Set<String>> = purchaseManager.ownedSoundPacks
    val soundPackPrices: StateFlow<Map<String, String>> = purchaseManager.soundPackPrices

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _temporaryCacheSizeBytes = MutableStateFlow(0L)
    val temporaryCacheSizeBytes: StateFlow<Long> = _temporaryCacheSizeBytes.asStateFlow()

    private val _storageBreakdown = MutableStateFlow(ScanRepository.StorageUsageBreakdown())
    val storageBreakdown: StateFlow<ScanRepository.StorageUsageBreakdown> = _storageBreakdown.asStateFlow()

    private val _backupProgress = MutableStateFlow<Float?>(null)
    val backupProgress: StateFlow<Float?> = _backupProgress.asStateFlow()

    private val _backupStatusText = MutableStateFlow<String?>(null)
    val backupStatusText: StateFlow<String?> = _backupStatusText.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                ToolRegistry.syncFromSavedCsv(
                    enabledIdsCsv = prefs.enabledToolIdsCsv,
                    orderedIdsCsv = prefs.orderedToolIdsCsv,
                    orderedSectionsCsv = prefs.orderedSectionsCsv
                )
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun launchSoundPackPurchase(activity: android.app.Activity, pack: com.example.billing.SoundPack) {
        purchaseManager.launchSoundPackPurchase(activity, pack.productId)
    }

    fun launchRemoveAdsPurchase(activity: android.app.Activity) {
        purchaseManager.launchRemoveAdsPurchase(activity)
    }

    fun toggleFavoriteTool(toolId: String) {
        viewModelScope.launch {
            val isPremium = isPro.value || preferences.value.isCustomizationAccessActive(isPro.value)
            val maxLimit = if (isPremium) null else com.example.domain.FREE_FAVORITES_LIMIT
            val (_, limitReached) = preferencesRepository.toggleFavoriteWithLimit(toolId, maxLimit)
            if (limitReached) {
                _statusBanner.value = "Free limit: up to ${com.example.domain.FREE_FAVORITES_LIMIT} Favorites. Upgrade to Premium for unlimited Favorites!"
            }
        }
    }

    fun toggleFavoriteFeature(
        context: Context,
        featureId: String,
        isCustomizationUnlocked: Boolean,
        onLimitReached: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val maxLimit = if (isCustomizationUnlocked) null else com.example.domain.FREE_FAVORITES_LIMIT
            val (changed, limitReached) = preferencesRepository.toggleFavoriteWithLimit(featureId, maxLimit)
            if (limitReached) {
                _statusBanner.value = "Free limit reached (${com.example.domain.FREE_FAVORITES_LIMIT} Favorites). Unlock Premium for unlimited Favorites!"
                onLimitReached()
            } else if (changed) {
                com.example.domain.FeatureRegistry.syncFavoriteDynamicShortcuts(
                    context = context,
                    favoriteIds = preferencesRepository.preferencesFlow.valueOrCurrent(preferences.value).effectiveFavoriteIds(isPro.value)
                )
            }
        }
    }

    private fun kotlinx.coroutines.flow.Flow<UserPreferences>.valueOrCurrent(fallback: UserPreferences): UserPreferences = fallback

    fun saveCustomSections(sections: List<com.example.domain.CustomHomeSection>) {
        viewModelScope.launch {
            preferencesRepository.saveCustomSectionsJson(
                com.example.domain.FeatureRegistry.serializeSectionsToJson(sections)
            )
        }
    }

    fun setHiddenFeatures(ids: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setHiddenFeatureIds(ids)
        }
    }

    fun setPerTileSize(featureId: String, size: com.example.domain.FeatureTileSize) {
        viewModelScope.launch {
            preferencesRepository.setPerTileSize(featureId, size)
        }
    }

    fun setPinnedTopFeatures(ids: List<String>) {
        viewModelScope.launch {
            preferencesRepository.setPinnedTopFeatureIds(ids)
        }
    }

    fun setStartTabIndex(index: Int) {
        viewModelScope.launch {
            preferencesRepository.setStartTabIndex(index)
            val tabName = when (index) {
                1 -> "Outdoors"
                2 -> "Library"
                3 -> "Settings"
                else -> "Home"
            }
            _statusBanner.value = "Start tab set to $tabName"
        }
    }

    fun setHomeFirstFeatureId(featureId: String) {
        viewModelScope.launch {
            preferencesRepository.setHomeFirstFeatureId(featureId)
        }
    }

    fun dismissCustomLayoutSavedBanner() {
        viewModelScope.launch {
            preferencesRepository.setCustomLayoutSavedBannerDismissed(true)
        }
    }

    fun saveNamedHomeLayout(name: String) {
        viewModelScope.launch {
            preferencesRepository.saveNamedHomeLayout(name)
            _statusBanner.value = "Saved layout \"$name\""
        }
    }

    fun applyNamedHomeLayout(layout: com.example.domain.SavedHomeLayout) {
        viewModelScope.launch {
            preferencesRepository.applyNamedHomeLayout(layout)
            _statusBanner.value = "Switched to \"${layout.name}\" layout"
        }
    }

    fun deleteNamedHomeLayout(layoutId: String, name: String) {
        viewModelScope.launch {
            preferencesRepository.deleteNamedHomeLayout(layoutId)
            _statusBanner.value = "Deleted \"$name\" layout"
        }
    }

    fun moveFavoriteTool(toolId: String, delta: Int) {
        val current = preferences.value.effectiveFavoriteIds(isPro.value)
        val idx = current.indexOf(toolId)
        if (idx == -1) return
        val targetIdx = (idx + delta).coerceIn(0, current.lastIndex)
        if (targetIdx == idx) return
        viewModelScope.launch {
            preferencesRepository.moveFavoriteTool(idx, targetIdx)
        }
    }

    fun recordRecentTool(toolId: String) {
        viewModelScope.launch {
            preferencesRepository.recordRecentTool(toolId)
        }
    }

    fun toggleHomeBlock(blockId: String, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.toggleHomeBlock(blockId, enabled)
        }
    }

    fun moveHomeBlock(fromIndex: Int, toIndex: Int) {
        val current = preferences.value.effectiveHomeBlockOrderList(isPro.value).toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        viewModelScope.launch {
            preferencesRepository.setHomeBlockOrder(current)
        }
    }

    fun setHomeTileSize(size: String) {
        viewModelScope.launch {
            preferencesRepository.setHomeTileSize(size)
        }
    }

    fun setHomeLayoutColumns(columns: Int) {
        viewModelScope.launch {
            preferencesRepository.setHomeLayoutColumns(columns)
        }
    }

    fun setSectionLayoutOverride(categoryName: String, columns: Int?) {
        viewModelScope.launch {
            preferencesRepository.setSectionLayoutOverride(categoryName, columns)
        }
    }

    fun setShowToolNames(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowToolNames(show)
        }
    }

    fun toggleOutdoorInterest(interest: String) {
        viewModelScope.launch {
            preferencesRepository.toggleOutdoorInterest(interest)
        }
    }

    // --- Appearance ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setSelectedThemeName(name: String) {
        viewModelScope.launch {
            val entitledPro = isPro.value
            if (entitledPro || name.equals("Lime Night", ignoreCase = true)) {
                preferencesRepository.setSelectedThemeName(name, forceEntitledPro = entitledPro)
            }
            if (name == "Classic Light") {
                preferencesRepository.setThemeMode(ThemeMode.LIGHT)
            } else if (preferences.value.themeMode == ThemeMode.LIGHT) {
                preferencesRepository.setThemeMode(ThemeMode.DARK)
            }
        }
    }

    fun setLaunchAnimationMode(mode: com.example.data.LaunchAnimationMode) {
        viewModelScope.launch {
            preferencesRepository.setLaunchAnimationMode(mode)
        }
    }

    fun setSelectedAppIconStyle(style: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedAppIconStyle(style)
        }
    }

    fun setTextScale(scale: Float) {
        viewModelScope.launch {
            preferencesRepository.setTextScale(scale)
        }
    }

    fun setReduceGlassEffects(reduce: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReduceGlassEffects(reduce)
        }
    }

    fun setReduceMotion(reduce: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReduceMotion(reduce)
        }
    }

    // --- Sounds & Haptics ---
    fun setTapSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTapSoundsEnabled(enabled)
        }
    }

    fun setTapSoundVolume(volume: Float) {
        viewModelScope.launch {
            preferencesRepository.setTapSoundVolume(volume)
        }
    }

    fun setScanBeepEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setScanBeepEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHapticsEnabled(enabled)
        }
    }

    fun setHapticStrength(strength: HapticStrength) {
        viewModelScope.launch {
            preferencesRepository.setHapticStrength(strength)
        }
    }

    // --- Customize Home ---
    fun applyHomePreset(preset: HomeNeedPreset) {
        ToolRegistry.applyPreset(preset)
        persistToolRegistryState(presetName = preset.label, presetDismissed = true)
        _statusBanner.value = "Applied \"${preset.label}\" tool selection"
    }

    fun dismissHomePresetBanner() {
        viewModelScope.launch {
            preferencesRepository.setHomeCustomization(presetDismissed = true)
        }
    }

    fun setToolEnabled(toolId: String, enabled: Boolean) {
        ToolRegistry.setToolEnabled(toolId, enabled)
        persistToolRegistryState()
    }

    fun moveToolOrder(fromIndex: Int, toIndex: Int) {
        ToolRegistry.moveTool(fromIndex, toIndex)
        persistToolRegistryState()
    }

    fun moveCategorySection(fromIndex: Int, toIndex: Int) {
        ToolRegistry.moveCategory(fromIndex, toIndex)
        persistToolRegistryState()
    }

    fun setCustomToolModesForTool(toolId: String, modeVisibilityPairs: List<Pair<String, Boolean>>) {
        viewModelScope.launch {
            preferencesRepository.setCustomToolModesForTool(toolId, modeVisibilityPairs)
        }
    }

    fun getOutdoorsSegmentConfigs(csv: String = preferences.value.outdoorsSegmentsCsv): List<OutdoorsSegmentConfig> {
        val defaultMap = linkedMapOf(
            "SOLUNAR" to ("Solunar" to true),
            "SUN_MOON" to ("Sun & Moon" to true),
            "WEATHER" to ("Weather" to true)
        )
        val parsed = mutableListOf<OutdoorsSegmentConfig>()
        csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            val parts = token.split(":")
            val id = parts.firstOrNull()?.uppercase() ?: return@forEach
            val enabled = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: true
            val title = defaultMap[id]?.first ?: id
            if (defaultMap.containsKey(id)) {
                parsed.add(OutdoorsSegmentConfig(id, title, enabled))
            }
        }
        defaultMap.forEach { (id, pair) ->
            if (parsed.none { it.id == id }) {
                parsed.add(OutdoorsSegmentConfig(id, pair.first, pair.second))
            }
        }
        return parsed
    }

    fun toggleOutdoorsSegment(id: String, enabled: Boolean) {
        val current = getOutdoorsSegmentConfigs().map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        // Keep at least 1 segment enabled
        if (current.none { it.enabled }) return
        saveOutdoorsSegments(current)
    }

    fun moveOutdoorsSegment(fromIndex: Int, toIndex: Int) {
        val current = getOutdoorsSegmentConfigs().toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        saveOutdoorsSegments(current)
    }

    private fun saveOutdoorsSegments(configs: List<OutdoorsSegmentConfig>) {
        val csv = configs.joinToString(",") { "${it.id}:${it.enabled}" }
        viewModelScope.launch {
            preferencesRepository.setHomeCustomization(outdoorsSegmentsCsv = csv)
        }
    }

    fun toggleScanShortcut(shortcutId: String) {
        val current = preferences.value.scanShortcutsList.toMutableList()
        if (current.contains(shortcutId)) {
            if (current.size <= 1) {
                _statusBanner.value = "Keep at least 1 shortcut in the Scan menu."
                return
            }
            current.remove(shortcutId)
        } else {
            if (current.size >= 4) {
                _statusBanner.value = "Up to 4 shortcuts can be pinned to the Scan menu."
                return
            }
            current.add(shortcutId)
        }
        viewModelScope.launch {
            preferencesRepository.setHomeCustomization(scanShortcutsCsv = current.joinToString(","))
        }
    }

    fun resetHomeCustomizationToDefault() {
        ToolRegistry.resetToDefault()
        viewModelScope.launch {
            preferencesRepository.resetHomeAndToolsCustomization()
            preferencesRepository.setHomeCustomization(
                preset = "",
                presetDismissed = false,
                enabledToolIdsCsv = ToolRegistry.tools.value.joinToString(",") { it.id },
                orderedToolIdsCsv = ToolRegistry.tools.value.joinToString(",") { it.id },
                orderedSectionsCsv = "MEDIA_STUDIO,PHONE_CARE_AND_AUDIO,MEASURE,LIGHT_AND_SOUND,TRAVEL_AND_TIME,CONVERT_AND_WRITE",
                outdoorsSegmentsCsv = "SOLUNAR:true,SUN_MOON:true,WEATHER:true",
                scanShortcutsCsv = "scan_doc,scan_qr,quick_note,flashlight"
            )
            _statusBanner.value = "Home & Tools layout reset to default"
        }
    }

    private fun persistToolRegistryState(
        presetName: String? = null,
        presetDismissed: Boolean? = null
    ) {
        val tools = ToolRegistry.tools.value
        val enabledCsv = tools.filter { it.isEnabled }.joinToString(",") { it.id }
        val orderedCsv = tools.joinToString(",") { it.id }
        val sectionsCsv = ToolRegistry.orderedCategories.value.joinToString(",") { it.name }
        viewModelScope.launch {
            preferencesRepository.setHomeCustomization(
                preset = presetName,
                presetDismissed = presetDismissed,
                enabledToolIdsCsv = enabledCsv,
                orderedToolIdsCsv = orderedCsv,
                orderedSectionsCsv = sectionsCsv
            )
        }
    }

    // --- Units & Formats ---
    fun setUnitSystem(units: UnitSystem) {
        viewModelScope.launch {
            preferencesRepository.setUnitSystem(units)
        }
    }

    fun setDistanceUnit(unit: String) {
        viewModelScope.launch {
            preferencesRepository.setDistanceUnit(unit)
        }
    }

    fun setSpeedUnit(unit: String) {
        viewModelScope.launch {
            preferencesRepository.setSpeedUnit(unit)
        }
    }

    fun setTemperatureUnit(unit: String) {
        viewModelScope.launch {
            preferencesRepository.setTemperatureUnit(unit)
        }
    }

    fun setPressureUnit(unit: String) {
        viewModelScope.launch {
            preferencesRepository.setPressureUnit(unit)
        }
    }

    fun setUse24HourTime(use24h: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUse24HourTime(use24h)
        }
    }

    fun setDefaultTimeZone(zone: String) {
        viewModelScope.launch {
            preferencesRepository.setDefaultTimeZone(zone)
        }
    }

    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch {
            preferencesRepository.setDefaultCurrency(currency)
        }
    }

    // --- Location ---
    fun setManualLocation(lat: Double, lng: Double, useManual: Boolean, label: String) {
        viewModelScope.launch {
            preferencesRepository.setManualLocation(lat, lng, useManual, label)
            _statusBanner.value = if (useManual) {
                "Location set to manual: $label (${String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)})"
            } else {
                "Location set to Automatic GPS"
            }
        }
    }

    // --- Notifications ---
    fun setSolunarNotificationConfig(
        enabled: Boolean? = null,
        quietHoursEnabled: Boolean? = null,
        quietHours: String? = null
    ) {
        viewModelScope.launch {
            preferencesRepository.setSolunarNotificationConfig(enabled, quietHoursEnabled, quietHours)
        }
    }

    fun setExpiryNotificationConfig(
        enabled: Boolean? = null,
        quietHoursEnabled: Boolean? = null,
        quietHours: String? = null
    ) {
        viewModelScope.launch {
            preferencesRepository.setExpiryNotificationConfig(enabled, quietHoursEnabled, quietHours)
        }
    }

    fun setDailyBonusNotificationConfig(
        enabled: Boolean? = null,
        quietHoursEnabled: Boolean? = null,
        quietHours: String? = null
    ) {
        viewModelScope.launch {
            preferencesRepository.setDailyBonusNotificationConfig(enabled, quietHoursEnabled, quietHours)
        }
    }

    // --- Privacy & Security ---
    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAppLockEnabled(enabled)
            _statusBanner.value = if (enabled) {
                "App Lock enabled"
            } else {
                "App Lock disabled"
            }
        }
    }

    fun setAppLockMethod(method: String) {
        viewModelScope.launch {
            preferencesRepository.setAppLockMethod(method)
        }
    }

    fun setAppLockOnLaunch(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAppLockOnLaunch(enabled)
        }
    }

    fun setAppLockTimeoutMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setAppLockTimeoutMinutes(minutes)
            _statusBanner.value = "Background lock timeout set to $minutes min"
        }
    }

    fun setHideAppPreviewInRecents(hide: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHideAppPreviewInRecents(hide)
            _statusBanner.value = if (hide) {
                "App preview hidden in Android Recents (FLAG_SECURE active)"
            } else {
                "App preview visible in Android Recents"
            }
        }
    }

    fun setLockDocumentsWithBiometrics(locked: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLockDocumentsWithBiometrics(locked)
            scanRepository?.setAllDocumentsLocked(locked)
            _statusBanner.value = if (locked) {
                "Biometric lock enabled for all documents"
            } else {
                "Biometric lock disabled for documents"
            }
        }
    }

    fun setLockNotesWithBiometrics(locked: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLockNotesWithBiometrics(locked)
            scanRepository?.setAllNotesLocked(locked)
            _statusBanner.value = if (locked) {
                "Biometric lock enabled for all notes & sketches"
            } else {
                "Biometric lock disabled for notes"
            }
        }
    }

    // --- Data & Backup ---
    fun refreshTemporaryCacheSize(context: Context) {
        viewModelScope.launch {
            val breakdown = scanRepository?.computeStorageBreakdown(context)
                ?: ScanRepository.StorageUsageBreakdown()
            _storageBreakdown.value = breakdown
            _temporaryCacheSizeBytes.value = breakdown.temporaryCacheBytes
        }
    }

    fun cleanTemporaryCache(context: Context) {
        viewModelScope.launch {
            val freed = scanRepository?.cleanTemporaryCache(context) ?: 0L
            val breakdown = scanRepository?.computeStorageBreakdown(context)
                ?: ScanRepository.StorageUsageBreakdown()
            _storageBreakdown.value = breakdown
            _temporaryCacheSizeBytes.value = breakdown.temporaryCacheBytes
            val formatted = ScanRepository.formatFileSize(freed.coerceAtLeast(0L))
            _statusBanner.value = "Cleaned temporary media & processing cache ($formatted freed)"
        }
    }

    fun exportBackupToUri(
        context: Context,
        uri: android.net.Uri,
        isFullBackup: Boolean
    ) {
        viewModelScope.launch {
            val repo = scanRepository
            if (repo == null) {
                _statusBanner.value = "Vault repository unavailable"
                return@launch
            }
            _backupProgress.value = 0.02f
            _backupStatusText.value = "Starting backup..."
            val result = repo.exportBackupZip(
                context = context,
                targetUri = uri,
                preferencesRepository = preferencesRepository,
                isFullBackup = isFullBackup,
                appVersion = com.example.BuildConfig.VERSION_NAME
            ) { progress, status ->
                _backupProgress.value = progress
                _backupStatusText.value = status
            }
            _backupProgress.value = null
            result.onSuccess { msg ->
                _backupStatusText.value = msg
                _statusBanner.value = msg
                refreshTemporaryCacheSize(context)
            }.onFailure { err ->
                val msg = "Backup failed: ${err.message ?: "Unknown error"}"
                _backupStatusText.value = msg
                _statusBanner.value = msg
            }
        }
    }

    fun restoreBackupFromUri(
        context: Context,
        uri: android.net.Uri,
        allowFullRestore: Boolean
    ) {
        viewModelScope.launch {
            val repo = scanRepository
            if (repo == null) {
                _statusBanner.value = "Vault repository unavailable"
                return@launch
            }
            _backupProgress.value = 0.05f
            _backupStatusText.value = "Reading backup ZIP..."
            val result = repo.restoreBackupZip(
                context = context,
                sourceUri = uri,
                preferencesRepository = preferencesRepository,
                allowFullRestore = allowFullRestore
            ) { progress, status ->
                _backupProgress.value = progress
                _backupStatusText.value = status
            }
            _backupProgress.value = null
            result.onSuccess { msg ->
                _backupStatusText.value = msg
                _statusBanner.value = msg
                refreshTemporaryCacheSize(context)
            }.onFailure { err ->
                val msg = "Restore failed: ${err.message ?: "Incompatible or corrupted ZIP"}"
                _backupStatusText.value = msg
                _statusBanner.value = msg
            }
        }
    }

    fun clearMediaRecentFilesHistory() {
        viewModelScope.launch {
            scanRepository?.clearAllMediaRecentFiles()
            _statusBanner.value = "Cleared Media Studio recent files history"
        }
    }

    fun clearAllLocalData(context: Context) {
        viewModelScope.launch {
            scanRepository?.clearAllLocalVaultData(context)
            preferencesRepository.clearAllPreferences()
            ToolRegistry.resetToDefault()
            _temporaryCacheSizeBytes.value = 0L
            _storageBreakdown.value = ScanRepository.StorageUsageBreakdown()
            _statusBanner.value = "Cleared all local scans, documents, notes & preferences"
        }
    }

    fun restorePurchases() {
        purchaseManager.restorePurchases()
        _statusBanner.value = "Checked Google Play & local entitlements"
    }

    fun showStatusMessage(message: String) {
        _statusBanner.value = message
    }

    fun claimRewardedAdCoins() {
        viewModelScope.launch {
            val (granted, balAndMsg) = preferencesRepository.claimRewardedAdCoins()
            val (newBalance, message) = balAndMsg
            if (granted) {
                scanRepository?.recordCoinTransaction(
                    delta = 10,
                    balanceAfter = newBalance,
                    reason = "Completed rewarded ad (+10 coins)"
                )
            }
            _statusBanner.value = message
        }
    }

    fun setSimulatePro(enabled: Boolean) {
        purchaseManager.setSimulatePro(enabled)
        _statusBanner.value = if (enabled) {
            "Simulate Pro ON — all AdMob ads disabled & Pro features unlocked"
        } else {
            "Simulate Pro OFF — free tier & AdMob test ads active"
        }
    }

    fun clearProDebugStateAndUnlocks() {
        purchaseManager.clearProDebugState()
        _statusBanner.value = "Cleared Pro debug state & all active 1-hour temporary unlocks"
    }

    class Factory(
        private val preferencesRepository: PreferencesRepository,
        private val purchaseManager: PurchaseManager,
        private val scanRepository: ScanRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferencesRepository, purchaseManager, scanRepository) as T
        }
    }
}
