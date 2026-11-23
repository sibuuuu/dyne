package com.example.ui.sunmoon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LocationProvider
import com.example.data.PreferencesRepository
import com.example.data.SensorRepository
import com.example.domain.AstroCalculator
import com.example.domain.CelestialPosition
import com.example.domain.DailyAstroReport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZonedDateTime

data class SunMoonUiState(
    val phoneHeadingDeg: Float = 0f,
    val sunPosition: CelestialPosition = CelestialPosition(180.0, 45.0),
    val moonPosition: CelestialPosition = CelestialPosition(95.0, 25.0),
    val dailyReport: DailyAstroReport = AstroCalculator.calculateDailyReport(LocalDate.now(), 37.7749, -122.4194),
    val use24HourTime: Boolean = false,
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194
)

class SunMoonViewModel(
    sensorRepository: SensorRepository,
    locationProvider: LocationProvider,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val tickerFlow: Flow<ZonedDateTime> = flow {
        while (true) {
            emit(ZonedDateTime.now())
            delay(15_000L)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coordsFlow = preferencesRepository.preferencesFlow.flatMapLatest { prefs ->
        if (prefs.useManualLocation) {
            kotlinx.coroutines.flow.flowOf(
                com.example.data.GeoCoordinates(
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

    val uiState: StateFlow<SunMoonUiState> = combine(
        sensorRepository.sensorFlow,
        coordsFlow,
        preferencesRepository.preferencesFlow,
        tickerFlow
    ) { sensor, coords, prefs, now ->
        val sunPos = AstroCalculator.getSunPosition(now, coords.latitude, coords.longitude)
        val moonPos = AstroCalculator.getMoonPosition(now, coords.latitude, coords.longitude)
        val report = AstroCalculator.calculateDailyReport(now.toLocalDate(), coords.latitude, coords.longitude)

        SunMoonUiState(
            phoneHeadingDeg = sensor.azimuthDeg,
            sunPosition = sunPos,
            moonPosition = moonPos,
            dailyReport = report,
            use24HourTime = prefs.use24HourTime,
            latitude = coords.latitude,
            longitude = coords.longitude
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SunMoonUiState()
    )

    class Factory(
        private val sensorRepository: SensorRepository,
        private val locationProvider: LocationProvider,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SunMoonViewModel(sensorRepository, locationProvider, preferencesRepository) as T
        }
    }
}
