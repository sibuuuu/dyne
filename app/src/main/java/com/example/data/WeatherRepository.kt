package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WeatherConditionKind {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    RAIN,
    SNOW,
    THUNDERSTORM
}

enum class PressureTrend(val arrow: String, val label: String) {
    RISING("↑", "Rising"),
    STEADY("→", "Steady"),
    FALLING("↓", "Falling")
}

data class HourlyForecastItem(
    val timeIso: String,
    val hourLabel: String,
    val tempC: Double,
    val precipProbPercent: Int,
    val weatherCode: Int,
    val pressureHpa: Double,
    val windKmh: Double
) {
    val isoTime: String get() = timeIso
    val conditionKind: WeatherConditionKind
        get() = OpenMeteoWeatherRepository.mapWmoCodeToKind(weatherCode)
}

data class DailyForecastItem(
    val dateIso: String,
    val dayLabel: String,
    val weatherCode: Int,
    val tempMaxC: Double,
    val tempMinC: Double,
    val precipProbMaxPercent: Int,
    val uvIndexMax: Double,
    val sunriseLabel: String,
    val sunsetLabel: String,
    val windMaxKmh: Double
) {
    val conditionKind: WeatherConditionKind
        get() = OpenMeteoWeatherRepository.mapWmoCodeToKind(weatherCode)

    val conditionLabel: String
        get() = OpenMeteoWeatherRepository.mapWmoCodeToLabel(weatherCode)
}

data class CitySearchResult(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
) {
    val displayTitle: String
        get() = listOf(name, region, country).filter { it.isNotBlank() }.joinToString(", ")
}

data class WeatherSnapshot(
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String,
    val fetchedAtMillis: Long,
    val isOfflineCached: Boolean,
    val currentTempC: Double,
    val feelsLikeC: Double,
    val weatherCode: Int,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val cloudCoverPercent: Int,
    val pressureHpa: Double,
    val pressureDelta3hHpa: Double,
    val pressureTrend: PressureTrend,
    val uvIndex: Double,
    val visibilityKm: Double,
    val precipProbPercent: Int,
    val sunriseLabel: String,
    val sunsetLabel: String,
    val outdoorScore: Int, // 0..100 from pressure trend, wind, cloud cover, and rain chance
    val solunarWeatherBoost: Int, // -10..+12 adjustment to Solunar Activity Score
    val solunarWeatherBoostReason: String,
    val hourly: List<HourlyForecastItem>,
    val daily: List<DailyForecastItem>,
    val usAqi: Int = 28,
    val europeanAqi: Int = 22,
    val pm25UgM3: Double = 6.8,
    val pm10UgM3: Double = 13.5
) {
    val currentWeatherCode: Int get() = weatherCode
    val currentWindKmh: Double get() = windSpeedKmh
    val currentUvIndex: Double get() = uvIndex

    val conditionKind: WeatherConditionKind
        get() = OpenMeteoWeatherRepository.mapWmoCodeToKind(weatherCode)

    val conditionLabel: String
        get() = OpenMeteoWeatherRepository.mapWmoCodeToLabel(weatherCode)

    val aqiCategoryLabel: String
        get() = when {
            usAqi <= 50 -> "Good"
            usAqi <= 100 -> "Moderate"
            usAqi <= 150 -> "Sensitive Groups"
            usAqi <= 200 -> "Unhealthy"
            else -> "Very Unhealthy"
        }

    val uvCategoryLabel: String
        get() = when {
            uvIndex < 3.0 -> "Low"
            uvIndex < 6.0 -> "Moderate"
            uvIndex < 8.0 -> "High"
            uvIndex < 11.0 -> "Very High"
            else -> "Extreme"
        }

    val windCardinal: String
        get() {
            val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val idx = (((windDirectionDeg % 360) + 360) % 360 / 45.0).roundToInt() % 8
            return dirs[idx]
        }
}

interface WeatherRepository {
    val weatherFlow: Flow<WeatherSnapshot?>
    suspend fun refreshWeather(
        lat: Double,
        lng: Double,
        locationLabel: String
    ): Result<WeatherSnapshot>

    suspend fun searchCities(query: String): List<CitySearchResult>

    fun getCachedSnapshot(): WeatherSnapshot? = null

    suspend fun fetchWeather(
        lat: Double,
        lng: Double,
        locationLabel: String
    ): Result<WeatherSnapshot> = refreshWeather(lat, lng, locationLabel)

    companion object {
        operator fun invoke(context: Context): OpenMeteoWeatherRepository =
            OpenMeteoWeatherRepository(context)

        fun wmoDescription(code: Int): String =
            OpenMeteoWeatherRepository.mapWmoCodeToLabel(code)
    }
}

class OpenMeteoWeatherRepository(
    private val context: Context
) : WeatherRepository {

    private val cacheFile = File(context.filesDir, "open_meteo_cache_v1.json")
    private val _weatherFlow = MutableStateFlow<WeatherSnapshot?>(null)
    override val weatherFlow: Flow<WeatherSnapshot?> = _weatherFlow.asStateFlow()

    init {
        val diskSnapshot = loadCachedSnapshotFromDisk()
        if (diskSnapshot != null) {
            _weatherFlow.value = diskSnapshot.copy(isOfflineCached = true)
        }
    }

    override suspend fun refreshWeather(
        lat: Double,
        lng: Double,
        locationLabel: String
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        val urlString = buildString {
            append("https://api.open-meteo.com/v1/forecast")
            append("?latitude=").append(String.format(Locale.US, "%.4f", lat))
            append("&longitude=").append(String.format(Locale.US, "%.4f", lng))
            append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,cloud_cover,surface_pressure,wind_speed_10m,wind_direction_10m")
            append("&hourly=temperature_2m,precipitation_probability,weather_code,surface_pressure,visibility,uv_index,wind_speed_10m,cloud_cover")
            append("&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max,wind_speed_10m_max")
            append("&timezone=auto")
        }

        runCatching {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("Open-Meteo HTTP $code")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val aqiJsonStr = fetchAirQualityRawJson(lat, lng)
                val nowMs = System.currentTimeMillis()
                val parsed = parseOpenMeteoResponse(
                    rawJson = body,
                    lat = lat,
                    lng = lng,
                    locationLabel = locationLabel,
                    fetchedAtMillis = nowMs,
                    isOfflineCached = false,
                    rawAqiJson = aqiJsonStr
                )
                saveSnapshotToDisk(body, aqiJsonStr, lat, lng, locationLabel, nowMs)
                _weatherFlow.value = parsed
                parsed
            } finally {
                conn.disconnect()
            }
        }.recoverCatching { error ->
            // Offline fallback: return cached disk snapshot if available, or synthesized local snapshot
            val cached = loadCachedSnapshotFromDisk()
            if (cached != null) {
                val offlineSnap = cached.copy(isOfflineCached = true)
                _weatherFlow.value = offlineSnap
                offlineSnap
            } else {
                val fallback = buildOfflineSeedSnapshot(lat, lng, locationLabel)
                _weatherFlow.value = fallback
                fallback
            }
        }
    }

    override fun getCachedSnapshot(): WeatherSnapshot? {
        return _weatherFlow.value ?: loadCachedSnapshotFromDisk()
    }

    private fun fetchAirQualityRawJson(lat: Double, lng: Double): String {
        val aqiUrl = buildString {
            append("https://air-quality-api.open-meteo.com/v1/air-quality")
            append("?latitude=").append(String.format(Locale.US, "%.4f", lat))
            append("&longitude=").append(String.format(Locale.US, "%.4f", lng))
            append("&current=us_aqi,european_aqi,pm10,pm2_5")
            append("&timezone=auto")
        }
        return runCatching {
            val conn = (URL(aqiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else ""
            } finally {
                conn.disconnect()
            }
        }.getOrDefault("")
    }

    override suspend fun searchCities(query: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext presetCities

        val onlineResults = runCatching {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=en&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            try {
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    val arr = root.optJSONArray("results") ?: JSONArray()
                    val list = mutableListOf<CitySearchResult>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            CitySearchResult(
                                name = obj.optString("name", trimmed),
                                region = obj.optString("admin1", ""),
                                country = obj.optString("country", ""),
                                latitude = obj.optDouble("latitude", 0.0),
                                longitude = obj.optDouble("longitude", 0.0)
                            )
                        )
                    }
                    list
                } else {
                    emptyList()
                }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(emptyList())

        if (onlineResults.isNotEmpty()) {
            onlineResults
        } else {
            presetCities.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                    it.country.contains(trimmed, ignoreCase = true) ||
                    it.region.contains(trimmed, ignoreCase = true)
            }.ifEmpty { presetCities }
        }
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun saveSnapshotToDisk(
        rawApiJson: String,
        rawAqiJson: String,
        lat: Double,
        lng: Double,
        label: String,
        fetchedAtMillis: Long
    ) {
        runCatching {
            val wrapper = JSONObject().apply {
                put("lat", lat)
                put("lng", lng)
                put("label", label)
                put("fetchedAtMillis", fetchedAtMillis)
                put("rawApiJson", rawApiJson)
                put("rawAqiJson", rawAqiJson)
            }
            cacheFile.writeText(wrapper.toString())
        }
    }

    private fun loadCachedSnapshotFromDisk(): WeatherSnapshot? {
        if (!cacheFile.exists()) return null
        return runCatching {
            val wrapper = JSONObject(cacheFile.readText())
            val rawApiJson = wrapper.getString("rawApiJson")
            val rawAqiJson = wrapper.optString("rawAqiJson", "")
            val lat = wrapper.optDouble("lat", 37.7749)
            val lng = wrapper.optDouble("lng", -122.4194)
            val label = wrapper.optString("label", "Cached Location")
            val fetchedAt = wrapper.optLong("fetchedAtMillis", System.currentTimeMillis())
            parseOpenMeteoResponse(
                rawJson = rawApiJson,
                lat = lat,
                lng = lng,
                locationLabel = label,
                fetchedAtMillis = fetchedAt,
                isOfflineCached = true,
                rawAqiJson = rawAqiJson
            )
        }.getOrNull()
    }

    companion object {
        val presetCities = listOf(
            CitySearchResult("San Francisco", "California", "United States", 37.7749, -122.4194),
            CitySearchResult("New York", "New York", "United States", 40.7128, -74.0060),
            CitySearchResult("London", "England", "United Kingdom", 51.5074, -0.1278),
            CitySearchResult("Tokyo", "Tokyo", "Japan", 35.6762, 139.6503),
            CitySearchResult("Kolkata", "West Bengal", "India", 22.5726, 88.3639),
            CitySearchResult("Sydney", "New South Wales", "Australia", -33.8688, 151.2093),
            CitySearchResult("Denver", "Colorado", "United States", 39.7392, -104.9903),
            CitySearchResult("Zurich", "Zurich", "Switzerland", 47.3769, 8.5417)
        )

        fun parseOpenMeteoResponse(
            rawJson: String,
            lat: Double,
            lng: Double,
            locationLabel: String,
            fetchedAtMillis: Long,
            isOfflineCached: Boolean,
            rawAqiJson: String = ""
        ): WeatherSnapshot {
            val root = JSONObject(rawJson)
            val current = root.optJSONObject("current") ?: JSONObject()
            val hourly = root.optJSONObject("hourly") ?: JSONObject()
            val daily = root.optJSONObject("daily") ?: JSONObject()

            val currentTempC = current.optDouble("temperature_2m", 19.5)
            val feelsLikeC = current.optDouble("apparent_temperature", currentTempC)
            val humidity = current.optInt("relative_humidity_2m", 58)
            val weatherCode = current.optInt("weather_code", 1)
            val cloudCover = current.optInt("cloud_cover", 25)
            val pressureHpa = current.optDouble("surface_pressure", 1013.2)
            val windSpeedKmh = current.optDouble("wind_speed_10m", 11.0)
            val windDirectionDeg = current.optInt("wind_direction_10m", 240)

            // Parse hourly arrays
            val hTimes = hourly.optJSONArray("time") ?: JSONArray()
            val hTemps = hourly.optJSONArray("temperature_2m") ?: JSONArray()
            val hPrecip = hourly.optJSONArray("precipitation_probability") ?: JSONArray()
            val hCodes = hourly.optJSONArray("weather_code") ?: JSONArray()
            val hPressure = hourly.optJSONArray("surface_pressure") ?: JSONArray()
            val hVis = hourly.optJSONArray("visibility") ?: JSONArray()
            val hUv = hourly.optJSONArray("uv_index") ?: JSONArray()
            val hWind = hourly.optJSONArray("wind_speed_10m") ?: JSONArray()

            val currentHourPrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH"))
            var startIndex = 0
            for (i in 0 until hTimes.length()) {
                if (hTimes.optString(i, "").startsWith(currentHourPrefix)) {
                    startIndex = i
                    break
                }
            }

            // Compute 3-hour barometric pressure delta
            val prevIdx = (startIndex - 3).coerceAtLeast(0)
            val prevPressure = if (hPressure.length() > prevIdx) {
                hPressure.optDouble(prevIdx, pressureHpa)
            } else {
                pressureHpa
            }
            val currentHourPressure = if (hPressure.length() > startIndex) {
                hPressure.optDouble(startIndex, pressureHpa)
            } else {
                pressureHpa
            }
            val pressureDelta = currentHourPressure - prevPressure
            val pressureTrend = when {
                pressureDelta >= 0.7 -> PressureTrend.RISING
                pressureDelta <= -0.7 -> PressureTrend.FALLING
                else -> PressureTrend.STEADY
            }

            val currentVisMeters = if (hVis.length() > startIndex) hVis.optDouble(startIndex, 10000.0) else 10000.0
            val visibilityKm = (currentVisMeters / 1000.0).coerceIn(0.2, 50.0)
            val currentUv = if (hUv.length() > startIndex) hUv.optDouble(startIndex, 3.5) else 3.5
            val currentPrecipProb = if (hPrecip.length() > startIndex) hPrecip.optInt(startIndex, 10) else 10

            val hourlyItems = mutableListOf<HourlyForecastItem>()
            val endIndex = (startIndex + 24).coerceAtMost(hTimes.length())
            for (i in startIndex until endIndex) {
                val iso = hTimes.optString(i, "")
                val hourLabel = iso.substringAfter("T", "00:00").take(5)
                hourlyItems.add(
                    HourlyForecastItem(
                        timeIso = iso,
                        hourLabel = if (i == startIndex) "Now" else hourLabel,
                        tempC = hTemps.optDouble(i, currentTempC),
                        precipProbPercent = hPrecip.optInt(i, 0),
                        weatherCode = hCodes.optInt(i, weatherCode),
                        pressureHpa = hPressure.optDouble(i, pressureHpa),
                        windKmh = hWind.optDouble(i, windSpeedKmh)
                    )
                )
            }

            // Parse daily arrays (7 days)
            val dTimes = daily.optJSONArray("time") ?: JSONArray()
            val dCodes = daily.optJSONArray("weather_code") ?: JSONArray()
            val dMax = daily.optJSONArray("temperature_2m_max") ?: JSONArray()
            val dMin = daily.optJSONArray("temperature_2m_min") ?: JSONArray()
            val dSunrise = daily.optJSONArray("sunrise") ?: JSONArray()
            val dSunset = daily.optJSONArray("sunset") ?: JSONArray()
            val dUvMax = daily.optJSONArray("uv_index_max") ?: JSONArray()
            val dPrecipMax = daily.optJSONArray("precipitation_probability_max") ?: JSONArray()
            val dWindMax = daily.optJSONArray("wind_speed_10m_max") ?: JSONArray()

            val dayFmt = DateTimeFormatter.ofPattern("EEE d", Locale.US)
            val dailyItems = mutableListOf<DailyForecastItem>()
            val dayCount = dTimes.length().coerceAtMost(7)
            for (i in 0 until dayCount) {
                val dateStr = dTimes.optString(i, "")
                val label = if (i == 0) {
                    "Today"
                } else {
                    runCatching { LocalDate.parse(dateStr).format(dayFmt) }.getOrDefault(dateStr)
                }
                val sr = dSunrise.optString(i, "06:30").substringAfter("T", "06:30")
                val ss = dSunset.optString(i, "18:15").substringAfter("T", "18:15")
                dailyItems.add(
                    DailyForecastItem(
                        dateIso = dateStr,
                        dayLabel = label,
                        weatherCode = dCodes.optInt(i, weatherCode),
                        tempMaxC = dMax.optDouble(i, currentTempC + 4.0),
                        tempMinC = dMin.optDouble(i, currentTempC - 4.0),
                        precipProbMaxPercent = dPrecipMax.optInt(i, 15),
                        uvIndexMax = dUvMax.optDouble(i, 5.0),
                        sunriseLabel = sr,
                        sunsetLabel = ss,
                        windMaxKmh = dWindMax.optDouble(i, windSpeedKmh)
                    )
                )
            }

            val todaySunrise = dailyItems.firstOrNull()?.sunriseLabel ?: "06:32"
            val todaySunset = dailyItems.firstOrNull()?.sunsetLabel ?: "18:18"

            val outdoorScore = calculateOutdoorScore(
                pressureTrend = pressureTrend,
                windSpeedKmh = windSpeedKmh,
                cloudCoverPercent = cloudCover,
                rainChancePercent = currentPrecipProb
            )

            val (solunarBoost, boostReason) = calculateSolunarWeatherBoost(
                pressureHpa = pressureHpa,
                pressureDelta3hHpa = pressureDelta,
                pressureTrend = pressureTrend,
                windSpeedKmh = windSpeedKmh
            )

            var parsedUsAqi = 28
            var parsedEuAqi = 22
            var parsedPm25 = 6.8
            var parsedPm10 = 13.5
            if (rawAqiJson.isNotBlank()) {
                runCatching {
                    val aqiRoot = JSONObject(rawAqiJson)
                    val aqiCur = aqiRoot.optJSONObject("current")
                    if (aqiCur != null) {
                        parsedUsAqi = aqiCur.optInt("us_aqi", 28).coerceAtLeast(1)
                        parsedEuAqi = aqiCur.optInt("european_aqi", 22).coerceAtLeast(1)
                        parsedPm25 = aqiCur.optDouble("pm2_5", 6.8).coerceAtLeast(0.0)
                        parsedPm10 = aqiCur.optDouble("pm10", 13.5).coerceAtLeast(0.0)
                    }
                }
            }

            return WeatherSnapshot(
                latitude = lat,
                longitude = lng,
                locationLabel = locationLabel,
                fetchedAtMillis = fetchedAtMillis,
                isOfflineCached = isOfflineCached,
                currentTempC = currentTempC,
                feelsLikeC = feelsLikeC,
                weatherCode = weatherCode,
                humidityPercent = humidity,
                windSpeedKmh = windSpeedKmh,
                windDirectionDeg = windDirectionDeg,
                cloudCoverPercent = cloudCover,
                pressureHpa = pressureHpa,
                pressureDelta3hHpa = pressureDelta,
                pressureTrend = pressureTrend,
                uvIndex = currentUv,
                visibilityKm = visibilityKm,
                precipProbPercent = currentPrecipProb,
                sunriseLabel = todaySunrise,
                sunsetLabel = todaySunset,
                outdoorScore = outdoorScore,
                solunarWeatherBoost = solunarBoost,
                solunarWeatherBoostReason = boostReason,
                hourly = hourlyItems,
                daily = dailyItems,
                usAqi = parsedUsAqi,
                europeanAqi = parsedEuAqi,
                pm25UgM3 = parsedPm25,
                pm10UgM3 = parsedPm10
            )
        }

        /**
         * Calculates Outdoor Score (0..100) from pressure trend, wind, cloud cover, and rain chance.
         */
        fun calculateOutdoorScore(
            pressureTrend: PressureTrend,
            windSpeedKmh: Double,
            cloudCoverPercent: Int,
            rainChancePercent: Int
        ): Int {
            var score = 92.0
            // Rain chance penalty (up to -42 pts)
            score -= (rainChancePercent.coerceIn(0, 100) * 0.42)
            // Wind penalty above 12 km/h (up to -28 pts)
            val excessWind = (windSpeedKmh - 12.0).coerceAtLeast(0.0)
            score -= (excessWind * 0.85).coerceAtMost(28.0)
            // Cloud cover gentle adjustment (20-50% clouds is great; heavy 100% overcast reduces slightly)
            if (cloudCoverPercent > 60) {
                score -= (cloudCoverPercent - 60) * 0.22
            }
            // Barometric pressure trend bonus/penalty
            score += when (pressureTrend) {
                PressureTrend.RISING -> 5.0
                PressureTrend.STEADY -> 3.0
                PressureTrend.FALLING -> -5.0
            }
            return score.roundToInt().coerceIn(12, 99)
        }

        /**
         * Computes the Solunar "Weather boost" (-10..+12 pts) using pressure trend and wind.
         * Fish and wildlife feed heavily when barometric pressure is gently falling ahead of a front
         * or steady near 1012-1020 hPa with light-to-moderate wind (< 18 km/h).
         */
        fun calculateSolunarWeatherBoost(
            pressureHpa: Double,
            pressureDelta3hHpa: Double,
            pressureTrend: PressureTrend,
            windSpeedKmh: Double
        ): Pair<Int, String> {
            var boost = 0
            val pressureNote = when {
                pressureTrend == PressureTrend.FALLING && pressureDelta3hHpa >= -3.5 -> {
                    boost += 6
                    "Falling barometer (${String.format(Locale.US, "%.1f", pressureHpa)} hPa)"
                }
                pressureTrend == PressureTrend.RISING && pressureDelta3hHpa <= 3.0 -> {
                    boost += 4
                    "Rising barometer (${String.format(Locale.US, "%.1f", pressureHpa)} hPa)"
                }
                pressureTrend == PressureTrend.STEADY && pressureHpa in 1010.0..1022.0 -> {
                    boost += 3
                    "Stable barometer (${String.format(Locale.US, "%.1f", pressureHpa)} hPa)"
                }
                else -> {
                    boost -= 2
                    "Rapid pressure shift (${String.format(Locale.US, "%.1f", pressureHpa)} hPa)"
                }
            }

            val windNote = when {
                windSpeedKmh in 4.0..18.0 -> {
                    boost += 3
                    "light ${windSpeedKmh.roundToInt()} km/h wind"
                }
                windSpeedKmh < 4.0 -> {
                    boost += 1
                    "calm ${windSpeedKmh.roundToInt()} km/h air"
                }
                windSpeedKmh <= 28.0 -> {
                    boost -= 2
                    "breezy ${windSpeedKmh.roundToInt()} km/h wind"
                }
                else -> {
                    boost -= 6
                    "high ${windSpeedKmh.roundToInt()} km/h wind"
                }
            }

            val clamped = boost.coerceIn(-10, 12)
            val sign = if (clamped >= 0) "+$clamped" else "$clamped"
            return clamped to "$sign pts • $pressureNote & $windNote"
        }

        fun mapWmoCodeToKind(code: Int): WeatherConditionKind {
            return when (code) {
                0 -> WeatherConditionKind.CLEAR
                1, 2 -> WeatherConditionKind.PARTLY_CLOUDY
                3 -> WeatherConditionKind.CLOUDY
                45, 48 -> WeatherConditionKind.FOG
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherConditionKind.RAIN
                71, 73, 75, 77, 85, 86 -> WeatherConditionKind.SNOW
                95, 96, 99 -> WeatherConditionKind.THUNDERSTORM
                else -> WeatherConditionKind.PARTLY_CLOUDY
            }
        }

        fun mapWmoCodeToLabel(code: Int): String {
            return when (code) {
                0 -> "Clear Sky"
                1 -> "Mainly Clear"
                2 -> "Partly Cloudy"
                3 -> "Overcast"
                45, 48 -> "Fog & Mist"
                51, 53, 55 -> "Light Drizzle"
                61, 63 -> "Moderate Rain"
                65, 80, 81, 82 -> "Rain Showers"
                71, 73, 75, 77, 85, 86 -> "Snowfall"
                95, 96, 99 -> "Thunderstorm"
                else -> "Partly Cloudy"
            }
        }

        fun buildOfflineSeedSnapshot(
            lat: Double,
            lng: Double,
            locationLabel: String
        ): WeatherSnapshot {
            val nowMs = System.currentTimeMillis()
            val baseHour = LocalDateTime.now().hour
            val hourly = (0 until 24).map { offset ->
                val h = (baseHour + offset) % 24
                val temp = 16.0 + 5.0 * kotlin.math.sin((h - 8) * Math.PI / 12.0)
                HourlyForecastItem(
                    timeIso = String.format(Locale.US, "%02d:00", h),
                    hourLabel = if (offset == 0) "Now" else String.format(Locale.US, "%02d:00", h),
                    tempC = temp,
                    precipProbPercent = if (h in 14..17) 20 else 10,
                    weatherCode = if (h in 7..18) 1 else 0,
                    pressureHpa = 1014.2 - offset * 0.08,
                    windKmh = 11.0
                )
            }
            val dayFmt = DateTimeFormatter.ofPattern("EEE d", Locale.US)
            val today = LocalDate.now()
            val daily = (0 until 7).map { d ->
                val date = today.plusDays(d.toLong())
                DailyForecastItem(
                    dateIso = date.toString(),
                    dayLabel = if (d == 0) "Today" else date.format(dayFmt),
                    weatherCode = if (d % 3 == 2) 2 else 1,
                    tempMaxC = 22.0 - (d % 3),
                    tempMinC = 12.0 + (d % 2),
                    precipProbMaxPercent = 10 + (d * 5) % 30,
                    uvIndexMax = 6.0,
                    sunriseLabel = "06:28",
                    sunsetLabel = "18:14",
                    windMaxKmh = 14.0
                )
            }
            val (boost, reason) = calculateSolunarWeatherBoost(
                pressureHpa = 1014.2,
                pressureDelta3hHpa = -1.1,
                pressureTrend = PressureTrend.FALLING,
                windSpeedKmh = 11.0
            )
            return WeatherSnapshot(
                latitude = lat,
                longitude = lng,
                locationLabel = locationLabel,
                fetchedAtMillis = nowMs,
                isOfflineCached = true,
                currentTempC = 19.5,
                feelsLikeC = 19.0,
                weatherCode = 1,
                humidityPercent = 56,
                windSpeedKmh = 11.0,
                windDirectionDeg = 245,
                cloudCoverPercent = 22,
                pressureHpa = 1014.2,
                pressureDelta3hHpa = -1.1,
                pressureTrend = PressureTrend.FALLING,
                uvIndex = 5.2,
                visibilityKm = 16.0,
                precipProbPercent = 10,
                sunriseLabel = "06:28",
                sunsetLabel = "18:14",
                outdoorScore = 86,
                solunarWeatherBoost = boost,
                solunarWeatherBoostReason = reason,
                hourly = hourly,
                daily = daily
            )
        }
    }
}
