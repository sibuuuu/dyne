package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
    val isFromGps: Boolean,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double = 0.0,
    val speedMetersPerSecond: Float = 0f,
    val timestampMillis: Long = System.currentTimeMillis(),
    val label: String = "GPS Fix"
)

class LocationProvider(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun locationUpdatesFlow(fallbackLat: Double, fallbackLng: Double): Flow<GeoCoordinates> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(
                GeoCoordinates(
                    latitude = fallbackLat,
                    longitude = fallbackLng,
                    isFromGps = false,
                    label = "Manual Fallback"
                )
            )
            awaitClose { }
            return@callbackFlow
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            trySend(
                GeoCoordinates(
                    latitude = fallbackLat,
                    longitude = fallbackLng,
                    isFromGps = false,
                    label = "Manual Fallback"
                )
            )
            awaitClose { }
            return@callbackFlow
        }

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLastKnown: Location? = null
        for (provider in providers) {
            runCatching {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null && (bestLastKnown == null || loc.time > bestLastKnown!!.time)) {
                        bestLastKnown = loc
                    }
                }
            }
        }

        if (bestLastKnown != null) {
            // Low-pass filter initial speed
            trySend(
                GeoCoordinates(
                    latitude = bestLastKnown!!.latitude,
                    longitude = bestLastKnown!!.longitude,
                    isFromGps = true,
                    accuracyMeters = bestLastKnown!!.accuracy,
                    altitudeMeters = bestLastKnown!!.altitude,
                    speedMetersPerSecond = bestLastKnown!!.speed.coerceAtLeast(0f),
                    timestampMillis = System.currentTimeMillis(),
                    label = "GPS Location"
                )
            )
        } else {
            trySend(
                GeoCoordinates(
                    latitude = fallbackLat,
                    longitude = fallbackLng,
                    isFromGps = false,
                    timestampMillis = System.currentTimeMillis(),
                    label = "Waiting for GPS"
                )
            )
        }

        var filteredSpeedMps = bestLastKnown?.speed?.coerceAtLeast(0f) ?: 0f

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val rawSpeed = if (location.hasSpeed()) location.speed.coerceAtLeast(0f) else 0f
                // Low-pass filter GPS speed readings (alpha = 0.35f)
                filteredSpeedMps = filteredSpeedMps + 0.35f * (rawSpeed - filteredSpeedMps)
                if (rawSpeed < 0.15f && filteredSpeedMps < 0.25f) {
                    filteredSpeedMps = 0f
                }
                trySend(
                    GeoCoordinates(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        isFromGps = true,
                        accuracyMeters = location.accuracy,
                        altitudeMeters = location.altitude,
                        speedMetersPerSecond = filteredSpeedMps,
                        timestampMillis = System.currentTimeMillis(),
                        label = "Live GPS"
                    )
                )
            }

            @Deprecated("Deprecated in API")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        runCatching {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            } else if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER,
                    500L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        }

        awaitClose {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }
}
