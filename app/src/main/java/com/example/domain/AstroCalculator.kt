package com.example.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

data class CelestialPosition(
    val azimuthDeg: Double,
    val altitudeDeg: Double,
    val distanceKm: Double = 0.0
)

data class SolunarPeriod(
    val title: String,
    val isMajor: Boolean,
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
    val peakTime: LocalTime,
    val startTime: LocalTime,
    val endTime: LocalTime
)

data class DailyAstroReport(
    val date: LocalDate,
    val moonPhaseFraction: Double, // 0.0 = New, 0.5 = Full, 1.0 = New
    val moonIlluminationPercent: Int, // 0..100%
    val moonPhaseName: String,
    val moonAgeDays: Double,
    val sunrise: LocalTime?,
    val sunset: LocalTime?,
    val solarNoon: LocalTime?,
    val moonrise: LocalTime?,
    val moonset: LocalTime?,
    val moonOverhead: LocalTime?,
    val moonUnderfoot: LocalTime?,
    val morningBlueHourStart: LocalTime?,
    val morningGoldenHourEnd: LocalTime?,
    val eveningGoldenHourStart: LocalTime?,
    val eveningBlueHourEnd: LocalTime?,
    val majorPeriods: List<SolunarPeriod>,
    val minorPeriods: List<SolunarPeriod>,
    val huntingScore: Int, // 0..100
    val fishingScore: Int, // 0..100
    val sunPathHourly: List<CelestialPosition> // 24 points from 00:00 to 23:00
) {
    fun scoreForMode(isFishing: Boolean): Int = if (isFishing) fishingScore else huntingScore
}

/**
 * Pure Kotlin offline astronomical & solunar calculator.
 * Implements Jean Meeus / USNO low-precision solar & lunar ephemeris equations.
 */
object AstroCalculator {

    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI
    private const val J1970 = 2440588.0
    private const val J2000 = 2451545.0
    private const val DAY_MS = 86400000.0
    private const val OBLIQUITY = 23.4397 * DEG_TO_RAD

    private fun toDays(epochMillis: Long): Double = epochMillis / DAY_MS - 0.5 + J1970 - J2000

    private fun rightAscension(l: Double, b: Double): Double =
        atan2(sin(l) * cos(OBLIQUITY) - tan(b) * sin(OBLIQUITY), cos(l))

    private fun declination(l: Double, b: Double): Double =
        asin(sin(b) * cos(OBLIQUITY) + cos(b) * sin(OBLIQUITY) * sin(l))

    private fun azimuth(h: Double, phi: Double, dec: Double): Double {
        // Returns compass azimuth (0° = North, 90° = East, 180° = South, 270° = West)
        val azSouth = atan2(sin(h), cos(h) * sin(phi) - tan(dec) * cos(phi))
        return ((azSouth * RAD_TO_DEG) + 180.0 + 360.0) % 360.0
    }

    private fun altitude(h: Double, phi: Double, dec: Double): Double =
        asin((sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(h)).coerceIn(-1.0, 1.0)) * RAD_TO_DEG

    private fun siderealTime(d: Double, lw: Double): Double =
        (280.16 + 360.9856235 * d) * DEG_TO_RAD - lw

    private fun solarMeanAnomaly(d: Double): Double =
        (357.5291 + 0.98560028 * d) * DEG_TO_RAD

    private fun eclipticLongitude(m: Double): Double {
        val c = (1.9148 * sin(m) + 0.02 * sin(2 * m) + 0.0003 * sin(3 * m)) * DEG_TO_RAD
        val p = 102.9372 * DEG_TO_RAD
        return m + c + p + PI
    }

    private data class Coords(val ra: Double, val dec: Double, val dist: Double = 149598000.0)

    private fun sunCoords(d: Double): Coords {
        val m = solarMeanAnomaly(d)
        val l = eclipticLongitude(m)
        return Coords(
            ra = rightAscension(l, 0.0),
            dec = declination(l, 0.0)
        )
    }

    private fun moonCoords(d: Double): Coords {
        val l = (218.316 + 13.176396 * d) * DEG_TO_RAD
        val m = (134.963 + 13.064993 * d) * DEG_TO_RAD
        val f = (93.272 + 13.229350 * d) * DEG_TO_RAD
        val dElong = (297.850 + 12.190749 * d) * DEG_TO_RAD

        // Ecliptic longitude & latitude with major periodic terms (evection, variation, annual inequality)
        val lon = l + (6.289 * sin(m) + 1.274 * sin(2 * dElong - m) + 0.658 * sin(2 * dElong) + 0.214 * sin(2 * m)) * DEG_TO_RAD
        val lat = (5.128 * sin(f)) * DEG_TO_RAD
        val dt = 385001.0 - 20905.0 * cos(m)

        return Coords(
            ra = rightAscension(lon, lat),
            dec = declination(lon, lat),
            dist = dt
        )
    }

    fun getSunPosition(dateTime: ZonedDateTime, lat: Double, lng: Double): CelestialPosition {
        val d = toDays(dateTime.toInstant().toEpochMilli())
        val lw = -lng * DEG_TO_RAD
        val phi = lat * DEG_TO_RAD
        val c = sunCoords(d)
        val h = siderealTime(d, lw) - c.ra
        return CelestialPosition(
            azimuthDeg = azimuth(h, phi, c.dec),
            altitudeDeg = altitude(h, phi, c.dec),
            distanceKm = c.dist
        )
    }

    fun getMoonPosition(dateTime: ZonedDateTime, lat: Double, lng: Double): CelestialPosition {
        val d = toDays(dateTime.toInstant().toEpochMilli())
        val lw = -lng * DEG_TO_RAD
        val phi = lat * DEG_TO_RAD
        val c = moonCoords(d)
        val h = siderealTime(d, lw) - c.ra
        var alt = altitude(h, phi, c.dec)
        // Parallax and atmospheric refraction adjustment
        alt += 0.017 / tan((alt + 10.26 / (alt + 5.10)).coerceAtLeast(1.0) * DEG_TO_RAD)
        return CelestialPosition(
            azimuthDeg = azimuth(h, phi, c.dec),
            altitudeDeg = alt,
            distanceKm = c.dist
        )
    }

    /**
     * Computes full Solunar and Sun/Moon ephemeris for a single calendar day.
     */
    fun calculateDailyReport(
        date: LocalDate,
        lat: Double,
        lng: Double,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyAstroReport {
        val startOfDay = date.atStartOfDay(zoneId)
        val midDay = startOfDay.plusHours(12)

        // Moon phase at local noon
        val dNoon = toDays(midDay.toInstant().toEpochMilli())
        val s = sunCoords(dNoon)
        val m = moonCoords(dNoon)
        val sdist = 149598000.0

        val phiPhase = acos(
            (sin(s.dec) * sin(m.dec) + cos(s.dec) * cos(m.dec) * cos(s.ra - m.ra)).coerceIn(-1.0, 1.0)
        )
        val inc = atan2(sdist * sin(phiPhase), m.dist - sdist * cos(phiPhase))
        val angle = atan2(
            cos(s.dec) * sin(s.ra - m.ra),
            sin(s.dec) * cos(m.dec) - cos(s.dec) * sin(m.dec) * cos(s.ra - m.ra)
        )
        val illumination = (1.0 + cos(inc)) / 2.0
        val phaseCycle = 0.5 + 0.5 * inc * (if (angle < 0) -1.0 else 1.0) / PI
        val normalizedPhase = ((phaseCycle % 1.0) + 1.0) % 1.0
        val synodicMonth = 29.53058867
        val moonAge = normalizedPhase * synodicMonth

        val phaseName = when {
            normalizedPhase < 0.03 || normalizedPhase > 0.97 -> "New Moon"
            normalizedPhase < 0.22 -> "Waxing Crescent"
            normalizedPhase < 0.28 -> "First Quarter"
            normalizedPhase < 0.47 -> "Waxing Gibbous"
            normalizedPhase < 0.53 -> "Full Moon"
            normalizedPhase < 0.72 -> "Waning Gibbous"
            normalizedPhase < 0.78 -> "Last Quarter"
            else -> "Waning Crescent"
        }

        // Sample Sun & Moon altitudes across the 24-hour day at 5-minute resolution (288 samples)
        // for ultra-reliable horizon crossing and culmination detection at all latitudes.
        val stepMinutes = 5
        val totalSamples = (24 * 60) / stepMinutes
        val sunAltitudes = DoubleArray(totalSamples + 1)
        val moonAltitudes = DoubleArray(totalSamples + 1)

        for (i in 0..totalSamples) {
            val t = startOfDay.plusMinutes((i * stepMinutes).toLong())
            sunAltitudes[i] = getSunPosition(t, lat, lng).altitudeDeg
            moonAltitudes[i] = getMoonPosition(t, lat, lng).altitudeDeg
        }

        val sunHorizon = -0.833
        val moonHorizon = 0.125

        val sunriseMin = findCrossingMinute(sunAltitudes, stepMinutes, sunHorizon, rising = true)
        val sunsetMin = findCrossingMinute(sunAltitudes, stepMinutes, sunHorizon, rising = false)
        val moonriseMin = findCrossingMinute(moonAltitudes, stepMinutes, moonHorizon, rising = true)
        val moonsetMin = findCrossingMinute(moonAltitudes, stepMinutes, moonHorizon, rising = false)

        // Golden Hour (-4° to +6° sun altitude) & Blue Hour (-6° to -4° sun altitude)
        val morningBlueStartMin = findCrossingMinute(sunAltitudes, stepMinutes, -6.0, rising = true)
        val morningGoldenEndMin = findCrossingMinute(sunAltitudes, stepMinutes, 6.0, rising = true)
        val eveningGoldenStartMin = findCrossingMinute(sunAltitudes, stepMinutes, 6.0, rising = false)
        val eveningBlueEndMin = findCrossingMinute(sunAltitudes, stepMinutes, -6.0, rising = false)

        // Solar Noon (highest sun altitude)
        var maxSunAlt = -999.0
        var solarNoonMin = 12 * 60
        for (i in 0..totalSamples) {
            if (sunAltitudes[i] > maxSunAlt) {
                maxSunAlt = sunAltitudes[i]
                solarNoonMin = i * stepMinutes
            }
        }

        // Moon Overhead (upper culmination / highest lunar altitude) & Moon Underfoot (lowest lunar altitude)
        var maxMoonAlt = -999.0
        var minMoonAlt = 999.0
        var moonOverheadMin = 12 * 60
        var moonUnderfootMin = 0
        for (i in 0..totalSamples) {
            if (moonAltitudes[i] > maxMoonAlt) {
                maxMoonAlt = moonAltitudes[i]
                moonOverheadMin = i * stepMinutes
            }
            if (moonAltitudes[i] < minMoonAlt) {
                minMoonAlt = moonAltitudes[i]
                moonUnderfootMin = i * stepMinutes
            }
        }

        // Ensure overhead & underfoot are ~12h25m apart if one wraps near midnight
        if (abs(moonOverheadMin - moonUnderfootMin) < 240) {
            moonUnderfootMin = (moonOverheadMin + 745) % 1440
        }

        // Build Major (~2h = 120m centered on Overhead & Underfoot) and Minor (~1h = 60m centered on Moonrise & Moonset) periods
        val majorPeriods = listOf(
            createSolunarPeriod("Major I (Lunar Transit)", isMajor = true, centerMin = moonOverheadMin, durationMin = 120),
            createSolunarPeriod("Major II (Opposite Transit)", isMajor = true, centerMin = moonUnderfootMin, durationMin = 120)
        ).sortedBy { it.startMinutesOfDay }

        val minorOneMin = moonriseMin ?: ((moonOverheadMin - 372 + 1440) % 1440)
        val minorTwoMin = moonsetMin ?: ((moonOverheadMin + 372) % 1440)
        val minorPeriods = listOf(
            createSolunarPeriod("Minor I (Moonrise)", isMajor = false, centerMin = minorOneMin, durationMin = 60),
            createSolunarPeriod("Minor II (Moonset)", isMajor = false, centerMin = minorTwoMin, durationMin = 60)
        ).sortedBy { it.startMinutesOfDay }

        // Compute Solunar Activity Score (0..100)
        // New Moon (phase ~0 or 1) and Full Moon (phase ~0.5) yield highest tidal/biological pull
        val phaseFactor = cos(normalizedPhase * 4.0 * PI) * 0.5 + 0.5 // Peaks at New (0), Full (0.5), Quarters secondary
        val newOrFullProximity = abs(cos(normalizedPhase * 2.0 * PI)) // 1.0 at New & Full, 0.0 at Quarters
        val baseScore = 44.0 + 36.0 * newOrFullProximity + 10.0 * phaseFactor

        // Bonus when any Solunar period falls within 60 mins of Sunrise or Sunset
        val allPeaks = listOf(moonOverheadMin, moonUnderfootMin, minorOneMin, minorTwoMin)
        var dawnDuskBonus = 0.0
        for (peak in allPeaks) {
            if (sunriseMin != null && abs(peak - sunriseMin) <= 75) dawnDuskBonus += 7.0
            if (sunsetMin != null && abs(peak - sunsetMin) <= 75) dawnDuskBonus += 7.0
        }

        val fishingScore = (baseScore + dawnDuskBonus).roundToInt().coerceIn(22, 99)
        // Hunting favors crisp crepuscular alignment & brighter moonlight slightly differently
        val huntingBonus = (illumination * 6.0) + (dawnDuskBonus * 1.1)
        val huntingScore = (baseScore - 3.0 + huntingBonus).roundToInt().coerceIn(20, 99)

        val sunPathHourly = (0..23).map { hour ->
            getSunPosition(startOfDay.plusHours(hour.toLong()), lat, lng)
        }

        return DailyAstroReport(
            date = date,
            moonPhaseFraction = normalizedPhase,
            moonIlluminationPercent = (illumination * 100.0).roundToInt().coerceIn(0, 100),
            moonPhaseName = phaseName,
            moonAgeDays = moonAge,
            sunrise = sunriseMin?.let { minutesToLocalTime(it) },
            sunset = sunsetMin?.let { minutesToLocalTime(it) },
            solarNoon = minutesToLocalTime(solarNoonMin),
            moonrise = moonriseMin?.let { minutesToLocalTime(it) },
            moonset = moonsetMin?.let { minutesToLocalTime(it) },
            moonOverhead = minutesToLocalTime(moonOverheadMin),
            moonUnderfoot = minutesToLocalTime(moonUnderfootMin),
            morningBlueHourStart = morningBlueStartMin?.let { minutesToLocalTime(it) },
            morningGoldenHourEnd = morningGoldenEndMin?.let { minutesToLocalTime(it) },
            eveningGoldenHourStart = eveningGoldenStartMin?.let { minutesToLocalTime(it) },
            eveningBlueHourEnd = eveningBlueEndMin?.let { minutesToLocalTime(it) },
            majorPeriods = majorPeriods,
            minorPeriods = minorPeriods,
            huntingScore = huntingScore,
            fishingScore = fishingScore,
            sunPathHourly = sunPathHourly
        )
    }

    fun calculateSevenDayForecast(
        startDate: LocalDate,
        lat: Double,
        lng: Double,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DailyAstroReport> {
        return (0 until 7).map { offset ->
            calculateDailyReport(startDate.plusDays(offset.toLong()), lat, lng, zoneId)
        }
    }

    private fun findCrossingMinute(
        samples: DoubleArray,
        stepMinutes: Int,
        thresholdDeg: Double,
        rising: Boolean
    ): Int? {
        for (i in 0 until samples.size - 1) {
            val a = samples[i] - thresholdDeg
            val b = samples[i + 1] - thresholdDeg
            if (rising && a < 0.0 && b >= 0.0) {
                val frac = abs(a) / (abs(a) + abs(b)).coerceAtLeast(1e-6)
                return ((i + frac) * stepMinutes).roundToInt().coerceIn(0, 1439)
            } else if (!rising && a >= 0.0 && b < 0.0) {
                val frac = abs(a) / (abs(a) + abs(b)).coerceAtLeast(1e-6)
                return ((i + frac) * stepMinutes).roundToInt().coerceIn(0, 1439)
            }
        }
        return null
    }

    private fun createSolunarPeriod(
        title: String,
        isMajor: Boolean,
        centerMin: Int,
        durationMin: Int
    ): SolunarPeriod {
        val half = durationMin / 2
        val startMin = (centerMin - half).coerceIn(0, 1439)
        val endMin = (centerMin + half).coerceIn(0, 1439)
        return SolunarPeriod(
            title = title,
            isMajor = isMajor,
            startMinutesOfDay = startMin,
            endMinutesOfDay = endMin,
            peakTime = minutesToLocalTime(centerMin.coerceIn(0, 1439)),
            startTime = minutesToLocalTime(startMin),
            endTime = minutesToLocalTime(endMin)
        )
    }

    private fun minutesToLocalTime(totalMinutes: Int): LocalTime {
        val clamped = ((totalMinutes % 1440) + 1440) % 1440
        return LocalTime.of(clamped / 60, clamped % 60)
    }
}
