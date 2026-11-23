package com.example.ui.solunar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CitySearchResult
import com.example.data.GeoCoordinates
import com.example.data.LocationProvider
import com.example.data.OpenMeteoWeatherRepository
import com.example.data.PreferencesRepository
import com.example.data.UnitSystem
import com.example.data.WeatherRepository
import com.example.data.WeatherSnapshot
import com.example.domain.AstroCalculator
import com.example.domain.DailyAstroReport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SolunarUiState(
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val locationLabel: String = "San Francisco, CA",
    val isFromGps: Boolean = false,
    val isFishingMode: Boolean = true,
    val use24HourTime: Boolean = true,
    val useCelsius: Boolean = true,
    val selectedDayIndex: Int = 0,
    val forecastDays: List<DailyAstroReport> = emptyList(),
    val weatherSnapshot: WeatherSnapshot? = null,
    val isLoadingWeather: Boolean = false
) {
    val activeReport: DailyAstroReport?
        get() = forecastDays.getOrNull(selectedDayIndex) ?: forecastDays.firstOrNull()

    val todayReport: DailyAstroReport?
        get() = forecastDays.firstOrNull()

    val weatherBoostPoints: Int
        get() = weatherSnapshot?.solunarWeatherBoost ?: 0

    val weatherBoostLine: String
        get() = weatherSnapshot?.solunarWeatherBoostReason
            ?: "+0 pts • Waiting for barometric & wind telemetry"

    val activeBaseScore: Int
        get() = activeReport?.scoreForMode(isFishingMode) ?: 72

    val activeAdjustedScore: Int
        get() {
            val base = activeBaseScore
            // Apply live weather boost primarily to Today (day 0) or scaled for upcoming days
            val boost = if (selectedDayIndex == 0) weatherBoostPoints else (weatherBoostPoints / 2)
            return (base + boost).coerceIn(15, 99)
        }

    val todayBestScore: Int
        get() {
            val base = todayReport?.scoreForMode(isFishingMode) ?: 76
            return (base + weatherBoostPoints).coerceIn(15, 99)
        }

    val todayBestWindowLabel: String
        get() {
            val report = todayReport ?: return "Major • 11:30 – 13:30"
            val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
            val upcomingMajor = report.majorPeriods.firstOrNull { it.endMinutesOfDay >= nowMin }
                ?: report.majorPeriods.firstOrNull()
            if (upcomingMajor == null) return "Major • 12:00 – 14:00"
            val pattern = if (use24HourTime) "HH:mm" else "h:mm a"
            val fmt = DateTimeFormatter.ofPattern(pattern, Locale.US)
            return "${upcomingMajor.startTime.format(fmt)} – ${upcomingMajor.endTime.format(fmt)}"
        }
}

class SolunarViewModel(
    private val locationProvider: LocationProvider,
    private val preferencesRepository: PreferencesRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _isFishingMode = MutableStateFlow(true)
    private val _selectedDayIndex = MutableStateFlow(0)
    private val _gpsRefreshTrigger = MutableStateFlow(0)
    private val _isLoadingWeather = MutableStateFlow(false)

    private val _showManualLocationSheet = MutableStateFlow(false)
    val showManualLocationSheet: StateFlow<Boolean> = _showManualLocationSheet.asStateFlow()

    private val _citySearchResults = MutableStateFlow<List<CitySearchResult>>(OpenMeteoWeatherRepository.presetCities)
    val citySearchResults: StateFlow<List<CitySearchResult>> = _citySearchResults.asStateFlow()

    private val _isSearchingCity = MutableStateFlow(false)
    val isSearchingCity: StateFlow<Boolean> = _isSearchingCity.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coordinatesFlow = combine(
        preferencesRepository.preferencesFlow,
        _gpsRefreshTrigger
    ) { prefs, _ -> prefs }
        .flatMapLatest { prefs ->
            if (prefs.useManualLocation) {
                flowOf(
                    GeoCoordinates(
                        latitude = prefs.manualLat,
                        longitude = prefs.manualLng,
                        isFromGps = false,
                        label = prefs.locationName
                    )
                )
            } else {
                locationProvider.locationUpdatesFlow(prefs.manualLat, prefs.manualLng)
            }
        }

    init {
        // Automatically fetch/refresh weather when coordinates change
        viewModelScope.launch {
            coordinatesFlow
                .distinctUntilChanged { a, b ->
                    kotlin.math.abs(a.latitude - b.latitude) < 0.01 &&
                        kotlin.math.abs(a.longitude - b.longitude) < 0.01 &&
                        a.label == b.label
                }
                .collect { coords ->
                    fetchWeatherForCoords(coords.latitude, coords.longitude, coords.label)
                }
        }
    }

    val uiState: StateFlow<SolunarUiState> = combine(
        coordinatesFlow,
        preferencesRepository.preferencesFlow,
        _isFishingMode,
        _selectedDayIndex,
        combine(weatherRepository.weatherFlow, _isLoadingWeather) { w, loading -> w to loading }
    ) { coords, prefs, isFishing, dayIdx, (weather, loading) ->
        val today = LocalDate.now()
        val sevenDays = AstroCalculator.calculateSevenDayForecast(
            startDate = today,
            lat = coords.latitude,
            lng = coords.longitude
        )
        SolunarUiState(
            latitude = coords.latitude,
            longitude = coords.longitude,
            locationLabel = coords.label,
            isFromGps = coords.isFromGps,
            isFishingMode = isFishing,
            use24HourTime = prefs.use24HourTime,
            useCelsius = prefs.unitSystem == UnitSystem.METRIC,
            selectedDayIndex = dayIdx.coerceIn(0, 6),
            forecastDays = sevenDays,
            weatherSnapshot = weather,
            isLoadingWeather = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SolunarUiState(
            forecastDays = AstroCalculator.calculateSevenDayForecast(LocalDate.now(), 37.7749, -122.4194),
            weatherSnapshot = OpenMeteoWeatherRepository.buildOfflineSeedSnapshot(37.7749, -122.4194, "Local Coordinates")
        )
    )

    fun setFishingMode(isFishing: Boolean) {
        _isFishingMode.value = isFishing
    }

    fun selectDayIndex(index: Int) {
        _selectedDayIndex.value = index.coerceIn(0, 6)
    }

    fun toggleCelsiusFahrenheit() {
        viewModelScope.launch {
            val next = if (uiState.value.useCelsius) UnitSystem.IMPERIAL else UnitSystem.METRIC
            preferencesRepository.setUnitSystem(next)
        }
    }

    fun refreshWeatherNow() {
        val cur = uiState.value
        fetchWeatherForCoords(cur.latitude, cur.longitude, cur.locationLabel)
    }

    private fun fetchWeatherForCoords(lat: Double, lng: Double, label: String) {
        viewModelScope.launch {
            _isLoadingWeather.value = true
            weatherRepository.refreshWeather(lat, lng, label)
            _isLoadingWeather.value = false
        }
    }

    fun refreshGpsLocation() {
        viewModelScope.launch {
            preferencesRepository.setManualLocation(
                lat = uiState.value.latitude,
                lng = uiState.value.longitude,
                useManual = false,
                label = "Live GPS"
            )
            _gpsRefreshTrigger.value += 1
            fetchWeatherForCoords(uiState.value.latitude, uiState.value.longitude, "Live GPS")
        }
    }

    fun saveManualCoordinates(lat: Double, lng: Double, label: String) {
        val cleanLabel = label.ifBlank { "Manual Coordinates" }
        viewModelScope.launch {
            preferencesRepository.setManualLocation(
                lat = lat,
                lng = lng,
                useManual = true,
                label = cleanLabel
            )
            _showManualLocationSheet.value = false
            fetchWeatherForCoords(lat, lng, cleanLabel)
        }
    }

    fun searchCities(query: String) {
        viewModelScope.launch {
            _isSearchingCity.value = true
            _citySearchResults.value = weatherRepository.searchCities(query)
            _isSearchingCity.value = false
        }
    }

    fun setShowManualSheet(show: Boolean) {
        _showManualLocationSheet.value = show
    }

    fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()

    class Factory(
        private val locationProvider: LocationProvider,
        private val preferencesRepository: PreferencesRepository,
        private val weatherRepository: WeatherRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SolunarViewModel(locationProvider, preferencesRepository, weatherRepository) as T
        }
    }
}
