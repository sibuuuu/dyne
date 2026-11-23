package com.example.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassSheet
import com.example.ui.components.HazeState
import com.example.ui.components.horizontalChipRowEdgeFade
import com.example.ui.components.pressScaleClickable
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class ConverterCategory(val title: String) {
    LENGTH("Length"),
    WEIGHT("Weight"),
    TEMPERATURE("Temperature"),
    AREA("Area"),
    VOLUME("Volume"),
    SPEED("Speed"),
    TIME("Time"),
    DATA("Data"),
    PRESSURE("Pressure"),
    ENERGY("Energy"),
    FUEL_ECONOMY("Fuel economy"),
    CURRENCY("Currency")
}

data class UnitDef(
    val code: String,
    val name: String,
    val symbol: String,
    val factorToBase: Double = 1.0 // Base unit factor for linear categories
)

data class FavoriteConversionPair(
    val categoryName: String,
    val fromCode: String,
    val toCode: String
) {
    val key: String get() = "$categoryName:$fromCode->$toCode"
}

data class ConversionHistoryEntry(
    val categoryName: String,
    val fromValue: String,
    val fromSymbol: String,
    val toValue: String,
    val toSymbol: String,
    val timestampMs: Long
)

object UnitCatalog {

    fun unitsFor(category: ConverterCategory): List<UnitDef> = when (category) {
        ConverterCategory.LENGTH -> listOf(
            UnitDef("m", "Meter", "m", 1.0),
            UnitDef("km", "Kilometer", "km", 1000.0),
            UnitDef("cm", "Centimeter", "cm", 0.01),
            UnitDef("mm", "Millimeter", "mm", 0.001),
            UnitDef("ft", "Foot", "ft", 0.3048),
            UnitDef("in", "Inch", "in", 0.0254),
            UnitDef("yd", "Yard", "yd", 0.9144),
            UnitDef("mi", "Mile", "mi", 1609.344),
            UnitDef("nmi", "Nautical Mile", "nmi", 1852.0)
        )

        ConverterCategory.WEIGHT -> listOf(
            UnitDef("kg", "Kilogram", "kg", 1.0),
            UnitDef("g", "Gram", "g", 0.001),
            UnitDef("mg", "Milligram", "mg", 0.000001),
            UnitDef("lb", "Pound", "lb", 0.45359237),
            UnitDef("oz", "Ounce", "oz", 0.028349523125),
            UnitDef("t", "Metric Tonne", "t", 1000.0),
            UnitDef("st", "Stone", "st", 6.35029318)
        )

        ConverterCategory.TEMPERATURE -> listOf(
            UnitDef("C", "Celsius", "°C"),
            UnitDef("F", "Fahrenheit", "°F"),
            UnitDef("K", "Kelvin", "K")
        )

        ConverterCategory.AREA -> listOf(
            UnitDef("m2", "Square Meter", "m²", 1.0),
            UnitDef("km2", "Square Kilometer", "km²", 1_000_000.0),
            UnitDef("cm2", "Square Centimeter", "cm²", 0.0001),
            UnitDef("ft2", "Square Foot", "ft²", 0.09290304),
            UnitDef("in2", "Square Inch", "in²", 0.00064516),
            UnitDef("yd2", "Square Yard", "yd²", 0.83612736),
            UnitDef("ac", "Acre", "ac", 4046.8564224),
            UnitDef("ha", "Hectare", "ha", 10_000.0)
        )

        ConverterCategory.VOLUME -> listOf(
            UnitDef("L", "Liter", "L", 1.0),
            UnitDef("mL", "Milliliter", "mL", 0.001),
            UnitDef("m3", "Cubic Meter", "m³", 1000.0),
            UnitDef("gal_us", "US Gallon", "gal (US)", 3.785411784),
            UnitDef("qt_us", "US Quart", "qt", 0.946352946),
            UnitDef("pt_us", "US Pint", "pt", 0.473176473),
            UnitDef("cup_us", "US Cup", "cup", 0.2365882365),
            UnitDef("fl_oz_us", "US Fluid Ounce", "fl oz", 0.02957352956),
            UnitDef("gal_uk", "Imperial Gallon", "gal (UK)", 4.54609)
        )

        ConverterCategory.SPEED -> listOf(
            UnitDef("mps", "Meters per second", "m/s", 1.0),
            UnitDef("kmh", "Kilometers per hour", "km/h", 1.0 / 3.6),
            UnitDef("mph", "Miles per hour", "mph", 0.44704),
            UnitDef("kn", "Knot", "kn", 1852.0 / 3600.0),
            UnitDef("fps", "Feet per second", "ft/s", 0.3048)
        )

        ConverterCategory.TIME -> listOf(
            UnitDef("s", "Second", "s", 1.0),
            UnitDef("ms", "Millisecond", "ms", 0.001),
            UnitDef("min", "Minute", "min", 60.0),
            UnitDef("h", "Hour", "h", 3600.0),
            UnitDef("d", "Day", "d", 86400.0),
            UnitDef("wk", "Week", "wk", 604800.0),
            UnitDef("yr", "Year (365d)", "yr", 31_536_000.0)
        )

        ConverterCategory.DATA -> listOf(
            UnitDef("B", "Byte", "B", 1.0),
            UnitDef("KB", "Kilobyte", "KB", 1024.0),
            UnitDef("MB", "Megabyte", "MB", 1024.0 * 1024.0),
            UnitDef("GB", "Gigabyte", "GB", 1024.0 * 1024.0 * 1024.0),
            UnitDef("TB", "Terabyte", "TB", 1024.0 * 1024.0 * 1024.0 * 1024.0),
            UnitDef("bit", "Bit", "bit", 0.125),
            UnitDef("Mbps", "Megabit", "Mbit", 125_000.0)
        )

        ConverterCategory.PRESSURE -> listOf(
            UnitDef("hPa", "Hectopascal", "hPa", 100.0),
            UnitDef("Pa", "Pascal", "Pa", 1.0),
            UnitDef("kPa", "Kilopascal", "kPa", 1000.0),
            UnitDef("bar", "Bar", "bar", 100_000.0),
            UnitDef("psi", "Pounds per sq. inch", "psi", 6894.75729),
            UnitDef("atm", "Standard Atmosphere", "atm", 101_325.0),
            UnitDef("mmHg", "Millimeter of Mercury", "mmHg", 133.322387415),
            UnitDef("inHg", "Inch of Mercury", "inHg", 3386.389)
        )

        ConverterCategory.ENERGY -> listOf(
            UnitDef("J", "Joule", "J", 1.0),
            UnitDef("kJ", "Kilojoule", "kJ", 1000.0),
            UnitDef("cal", "Calorie", "cal", 4.184),
            UnitDef("kcal", "Kilocalorie (Food)", "kcal", 4184.0),
            UnitDef("Wh", "Watt-hour", "Wh", 3600.0),
            UnitDef("kWh", "Kilowatt-hour", "kWh", 3_600_000.0),
            UnitDef("BTU", "British Thermal Unit", "BTU", 1055.05585)
        )

        ConverterCategory.FUEL_ECONOMY -> listOf(
            UnitDef("l_100km", "Liters per 100 km", "L/100km"),
            UnitDef("km_l", "Kilometers per Liter", "km/L"),
            UnitDef("mpg_us", "Miles per Gallon (US)", "mpg (US)"),
            UnitDef("mpg_uk", "Miles per Gallon (UK)", "mpg (UK)")
        )

        ConverterCategory.CURRENCY -> listOf(
            UnitDef("USD", "US Dollar", "USD"),
            UnitDef("EUR", "Euro", "EUR"),
            UnitDef("GBP", "British Pound", "GBP"),
            UnitDef("INR", "Indian Rupee", "INR"),
            UnitDef("JPY", "Japanese Yen", "JPY"),
            UnitDef("AUD", "Australian Dollar", "AUD"),
            UnitDef("CAD", "Canadian Dollar", "CAD"),
            UnitDef("CHF", "Swiss Franc", "CHF"),
            UnitDef("CNY", "Chinese Yuan", "CNY"),
            UnitDef("SGD", "Singapore Dollar", "SGD"),
            UnitDef("NZD", "New Zealand Dollar", "NZD"),
            UnitDef("SEK", "Swedish Krona", "SEK"),
            UnitDef("NOK", "Norwegian Krone", "NOK"),
            UnitDef("MXN", "Mexican Peso", "MXN"),
            UnitDef("BRL", "Brazilian Real", "BRL"),
            UnitDef("ZAR", "South African Rand", "ZAR"),
            UnitDef("HKD", "Hong Kong Dollar", "HKD"),
            UnitDef("KRW", "South Korean Won", "KRW")
        )
    }

    fun convert(
        category: ConverterCategory,
        value: Double,
        from: UnitDef,
        to: UnitDef,
        eurRates: Map<String, Double>
    ): Double {
        if (from.code == to.code) return value
        return when (category) {
            ConverterCategory.TEMPERATURE -> {
                val celsius = when (from.code) {
                    "C" -> value
                    "F" -> (value - 32.0) * 5.0 / 9.0
                    "K" -> value - 273.15
                    else -> value
                }
                when (to.code) {
                    "C" -> celsius
                    "F" -> celsius * 9.0 / 5.0 + 32.0
                    "K" -> celsius + 273.15
                    else -> celsius
                }
            }

            ConverterCategory.FUEL_ECONOMY -> {
                if (value <= 0.0) return 0.0
                // Convert input to km/L first
                val kmPerLiter = when (from.code) {
                    "km_l" -> value
                    "l_100km" -> 100.0 / value
                    "mpg_us" -> value * 0.425143707
                    "mpg_uk" -> value * 0.35400619
                    else -> value
                }
                if (kmPerLiter <= 0.0) return 0.0
                when (to.code) {
                    "km_l" -> kmPerLiter
                    "l_100km" -> 100.0 / kmPerLiter
                    "mpg_us" -> kmPerLiter / 0.425143707
                    "mpg_uk" -> kmPerLiter / 0.35400619
                    else -> kmPerLiter
                }
            }

            ConverterCategory.CURRENCY -> {
                val fromRate = if (from.code == "EUR") 1.0 else (eurRates[from.code] ?: 1.0)
                val toRate = if (to.code == "EUR") 1.0 else (eurRates[to.code] ?: 1.0)
                if (fromRate <= 0.0) 0.0 else (value / fromRate) * toRate
            }

            else -> {
                val inBase = value * from.factorToBase
                if (to.factorToBase == 0.0) 0.0 else inBase / to.factorToBase
            }
        }
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val absVal = abs(value)
        return when {
            absVal == 0.0 -> "0"
            absVal >= 10_000.0 -> String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
            absVal >= 1.0 -> String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
            absVal >= 0.0001 -> String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
            else -> String.format(Locale.US, "%.4e", value)
        }
    }
}

private object FrankfurterCurrencyStore {
    private const val CACHE_FILE = "frankfurter_rates_cache.json"
    private const val PREFS_FILE = "unit_converter_prefs.json"

    val defaultEurRates = mapOf(
        "EUR" to 1.0,
        "USD" to 1.085,
        "GBP" to 0.856,
        "INR" to 90.45,
        "JPY" to 164.20,
        "AUD" to 1.652,
        "CAD" to 1.482,
        "CHF" to 0.962,
        "CNY" to 7.84,
        "SGD" to 1.458,
        "NZD" to 1.795,
        "SEK" to 11.42,
        "NOK" to 11.68,
        "MXN" to 18.45,
        "BRL" to 5.56,
        "ZAR" to 20.12,
        "HKD" to 8.48,
        "KRW" to 1475.0
    )

    data class CachedCurrencyState(
        val rates: Map<String, Double>,
        val dateLabel: String,
        val fetchedAtMs: Long,
        val isFromLiveNetwork: Boolean
    )

    fun loadCachedRates(context: Context): CachedCurrencyState {
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.exists()) {
            return CachedCurrencyState(
                rates = defaultEurRates,
                dateLabel = "Offline baseline",
                fetchedAtMs = 0L,
                isFromLiveNetwork = false
            )
        }
        return runCatching {
            val json = JSONObject(file.readText())
            val dateLabel = json.optString("date", "Cached")
            val fetchedAtMs = json.optLong("fetchedAtMs", 0L)
            val ratesObj = json.optJSONObject("rates")
            val map = defaultEurRates.toMutableMap()
            if (ratesObj != null) {
                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = ratesObj.optDouble(k, map[k] ?: 1.0)
                }
            }
            CachedCurrencyState(
                rates = map,
                dateLabel = dateLabel,
                fetchedAtMs = fetchedAtMs,
                isFromLiveNetwork = false
            )
        }.getOrElse {
            CachedCurrencyState(
                rates = defaultEurRates,
                dateLabel = "Offline baseline",
                fetchedAtMs = 0L,
                isFromLiveNetwork = false
            )
        }
    }

    suspend fun fetchLatestFromFrankfurter(context: Context): CachedCurrencyState = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://api.frankfurter.app/latest?from=EUR",
            "https://api.frankfurter.dev/v1/latest?base=EUR"
        )
        for (endpoint in urls) {
            val result = runCatching {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val dateStr = json.optString("date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                    val ratesJson = json.getJSONObject("rates")
                    val merged = defaultEurRates.toMutableMap()
                    merged["EUR"] = 1.0
                    val keys = ratesJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        merged[key] = ratesJson.optDouble(key, merged[key] ?: 1.0)
                    }
                    val now = System.currentTimeMillis()
                    val cacheObj = JSONObject().apply {
                        put("date", dateStr)
                        put("fetchedAtMs", now)
                        val rObj = JSONObject()
                        merged.forEach { (k, v) -> rObj.put(k, v) }
                        put("rates", rObj)
                    }
                    File(context.filesDir, CACHE_FILE).writeText(cacheObj.toString())
                    CachedCurrencyState(
                        rates = merged,
                        dateLabel = dateStr,
                        fetchedAtMs = now,
                        isFromLiveNetwork = true
                    )
                } else {
                    null
                }
            }.getOrNull()
            if (result != null) return@withContext result
        }
        loadCachedRates(context)
    }

    fun loadFavoritesAndHistory(
        context: Context
    ): Pair<List<FavoriteConversionPair>, List<ConversionHistoryEntry>> {
        val file = File(context.filesDir, PREFS_FILE)
        if (!file.exists()) {
            val defaultFavs = listOf(
                FavoriteConversionPair(ConverterCategory.LENGTH.name, "m", "ft"),
                FavoriteConversionPair(ConverterCategory.WEIGHT.name, "kg", "lb"),
                FavoriteConversionPair(ConverterCategory.TEMPERATURE.name, "C", "F"),
                FavoriteConversionPair(ConverterCategory.CURRENCY.name, "USD", "EUR")
            )
            return defaultFavs to emptyList()
        }
        return runCatching {
            val root = JSONObject(file.readText())
            val favArr = root.optJSONArray("favorites") ?: JSONArray()
            val favs = mutableListOf<FavoriteConversionPair>()
            for (i in 0 until favArr.length()) {
                val item = favArr.getJSONObject(i)
                favs.add(
                    FavoriteConversionPair(
                        categoryName = item.optString("categoryName"),
                        fromCode = item.optString("fromCode"),
                        toCode = item.optString("toCode")
                    )
                )
            }
            val histArr = root.optJSONArray("history") ?: JSONArray()
            val hist = mutableListOf<ConversionHistoryEntry>()
            for (i in 0 until histArr.length()) {
                val item = histArr.getJSONObject(i)
                hist.add(
                    ConversionHistoryEntry(
                        categoryName = item.optString("categoryName"),
                        fromValue = item.optString("fromValue"),
                        fromSymbol = item.optString("fromSymbol"),
                        toValue = item.optString("toValue"),
                        toSymbol = item.optString("toSymbol"),
                        timestampMs = item.optLong("timestampMs")
                    )
                )
            }
            favs to hist
        }.getOrElse {
            emptyList<FavoriteConversionPair>() to emptyList()
        }
    }

    fun saveFavoritesAndHistory(
        context: Context,
        favorites: List<FavoriteConversionPair>,
        history: List<ConversionHistoryEntry>
    ) {
        runCatching {
            val root = JSONObject()
            val favArr = JSONArray()
            favorites.forEach { fav ->
                favArr.put(
                    JSONObject().apply {
                        put("categoryName", fav.categoryName)
                        put("fromCode", fav.fromCode)
                        put("toCode", fav.toCode)
                    }
                )
            }
            val histArr = JSONArray()
            history.take(15).forEach { h ->
                histArr.put(
                    JSONObject().apply {
                        put("categoryName", h.categoryName)
                        put("fromValue", h.fromValue)
                        put("fromSymbol", h.fromSymbol)
                        put("toValue", h.toValue)
                        put("toSymbol", h.toSymbol)
                        put("timestampMs", h.timestampMs)
                    }
                )
            }
            root.put("favorites", favArr)
            root.put("history", histArr)
            File(context.filesDir, PREFS_FILE).writeText(root.toString())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnitConverterInstrumentScreen(
    hazeState: HazeState? = null,
    forcedModeId: String? = null
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val scope = rememberCoroutineScope()

    val mappedForcedCategory = remember(forcedModeId) {
        when (forcedModeId?.lowercase(Locale.US)) {
            "length" -> ConverterCategory.LENGTH
            "weight" -> ConverterCategory.WEIGHT
            "temp", "temperature" -> ConverterCategory.TEMPERATURE
            "currency" -> ConverterCategory.CURRENCY
            "area" -> ConverterCategory.AREA
            "volume" -> ConverterCategory.VOLUME
            "speed" -> ConverterCategory.SPEED
            "data" -> ConverterCategory.DATA
            "pressure" -> ConverterCategory.PRESSURE
            "energy" -> ConverterCategory.ENERGY
            "fuel", "fuel_economy" -> ConverterCategory.FUEL_ECONOMY
            "time" -> ConverterCategory.TIME
            else -> null
        }
    }

    var selectedCategory by rememberSaveable {
        mutableStateOf(mappedForcedCategory ?: ConverterCategory.LENGTH)
    }
    LaunchedEffect(mappedForcedCategory) {
        if (mappedForcedCategory != null) {
            selectedCategory = mappedForcedCategory
        }
    }
    val units = remember(selectedCategory) { UnitCatalog.unitsFor(selectedCategory) }

    var fromUnitCode by rememberSaveable(selectedCategory) {
        mutableStateOf(units.first().code)
    }
    var toUnitCode by rememberSaveable(selectedCategory) {
        mutableStateOf(units.getOrElse(1) { units.first() }.code)
    }

    val fromUnit = remember(units, fromUnitCode) {
        units.firstOrNull { it.code == fromUnitCode } ?: units.first()
    }
    val toUnit = remember(units, toUnitCode) {
        units.firstOrNull { it.code == toUnitCode } ?: units.getOrElse(1) { units.first() }
    }

    val eurRates = remember { mutableStateMapOf<String, Double>() }
    var currencyLastUpdatedLabel by remember { mutableStateOf("Cached") }
    var currencyFetchedAtMs by remember { mutableStateOf(0L) }
    var currencyOnline by remember { mutableStateOf(false) }
    var isRefreshingRates by remember { mutableStateOf(false) }

    val favorites = remember { mutableStateListOf<FavoriteConversionPair>() }
    val history = remember { mutableStateListOf<ConversionHistoryEntry>() }

    var topFieldText by rememberSaveable { mutableStateOf("1") }
    var bottomFieldText by rememberSaveable { mutableStateOf("") }
    var activeEditedSide by rememberSaveable { mutableIntStateOf(0) } // 0 = top drives bottom, 1 = bottom drives top

    var pickerTargetSide by remember { mutableStateOf<Int?>(null) } // 0 = top unit picker, 1 = bottom unit picker
    var statusBanner by remember { mutableStateOf<String?>(null) }

    // Load persisted cache, favorites, and history
    LaunchedEffect(Unit) {
        val cached = FrankfurterCurrencyStore.loadCachedRates(context)
        eurRates.clear()
        eurRates.putAll(cached.rates)
        currencyLastUpdatedLabel = cached.dateLabel
        currencyFetchedAtMs = cached.fetchedAtMs
        currencyOnline = cached.isFromLiveNetwork

        val (loadedFavs, loadedHist) = FrankfurterCurrencyStore.loadFavoritesAndHistory(context)
        favorites.clear()
        favorites.addAll(loadedFavs)
        history.clear()
        history.addAll(loadedHist)

        // Refresh Frankfurter rates in background
        isRefreshingRates = true
        val live = FrankfurterCurrencyStore.fetchLatestFromFrankfurter(context)
        eurRates.clear()
        eurRates.putAll(live.rates)
        currencyLastUpdatedLabel = live.dateLabel
        currencyFetchedAtMs = live.fetchedAtMs
        currencyOnline = live.isFromLiveNetwork
        isRefreshingRates = false
    }

    // Recompute conversion whenever units, category, or rates change
    fun recomputeFromTop(rawTop: String) {
        val num = rawTop.toDoubleOrNull()
        if (num == null) {
            bottomFieldText = ""
        } else {
            val out = UnitCatalog.convert(selectedCategory, num, fromUnit, toUnit, eurRates)
            bottomFieldText = UnitCatalog.formatNumber(out)
        }
    }

    fun recomputeFromBottom(rawBottom: String) {
        val num = rawBottom.toDoubleOrNull()
        if (num == null) {
            topFieldText = ""
        } else {
            val out = UnitCatalog.convert(selectedCategory, num, toUnit, fromUnit, eurRates)
            topFieldText = UnitCatalog.formatNumber(out)
        }
    }

    LaunchedEffect(selectedCategory, fromUnit.code, toUnit.code, eurRates.size, currencyFetchedAtMs) {
        if (activeEditedSide == 0) {
            recomputeFromTop(topFieldText)
        } else {
            recomputeFromBottom(bottomFieldText)
        }
    }

    val currentPair = FavoriteConversionPair(selectedCategory.name, fromUnit.code, toUnit.code)
    val isCurrentFavorite = favorites.any { it.key == currentPair.key }

    fun recordCurrentToHistory() {
        if (topFieldText.isBlank() || bottomFieldText.isBlank()) return
        val entry = ConversionHistoryEntry(
            categoryName = selectedCategory.title,
            fromValue = topFieldText,
            fromSymbol = fromUnit.symbol,
            toValue = bottomFieldText,
            toSymbol = toUnit.symbol,
            timestampMs = System.currentTimeMillis()
        )
        history.removeAll {
            it.categoryName == entry.categoryName &&
                it.fromValue == entry.fromValue &&
                it.fromSymbol == entry.fromSymbol &&
                it.toSymbol == entry.toSymbol
        }
        history.add(0, entry)
        if (history.size > 12) {
            history.removeAt(history.lastIndex)
        }
        FrankfurterCurrencyStore.saveFavoritesAndHistory(context, favorites, history)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 12 Category horizontal pill selector (hidden when opened as a dedicated single-category feature)
        if (forcedModeId == null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalChipRowEdgeFade()
            ) {
                items(ConverterCategory.entries) { category ->
                    val selected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) theme.accent else theme.elevated)
                            .border(
                                1.dp,
                                if (selected) theme.accent else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(18.dp)
                            )
                            .pressScaleClickable(
                                onClick = {
                                    FeedbackHelper.tick(context)
                                    selectedCategory = category
                                    val catUnits = UnitCatalog.unitsFor(category)
                                    fromUnitCode = catUnits.first().code
                                    toUnitCode = catUnits.getOrElse(1) { catUnits.first() }.code
                                    activeEditedSide = 0
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("converter_cat_${category.name.lowercase()}")
                    ) {
                        Text(
                            text = category.title,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) BentoTextBlack else theme.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Currency live / cached status bar when Currency is selected
        if (selectedCategory == ConverterCategory.CURRENCY) {
            val timeFormatted = remember(currencyFetchedAtMs, currencyLastUpdatedLabel) {
                if (currencyFetchedAtMs > 0L) {
                    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.US)
                    "ECB / Frankfurter ($currencyLastUpdatedLabel • ${fmt.format(Date(currencyFetchedAtMs))})"
                } else {
                    "Offline cached rates ($currencyLastUpdatedLabel)"
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.elevated)
                    .border(1.dp, theme.bentoSky.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (currencyOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = "Currency status",
                        tint = if (currencyOnline) theme.bentoLime else theme.bentoSky,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Last updated: $timeFormatted",
                            style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = if (isRefreshingRates) "Refreshing Frankfurter rates..." else "Works offline from local cache",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(theme.surface)
                        .pressScaleClickable(
                            onClick = {
                                scope.launch {
                                    isRefreshingRates = true
                                    val refreshed = FrankfurterCurrencyStore.fetchLatestFromFrankfurter(context)
                                    eurRates.clear()
                                    eurRates.putAll(refreshed.rates)
                                    currencyLastUpdatedLabel = refreshed.dateLabel
                                    currencyFetchedAtMs = refreshed.fetchedAtMs
                                    currencyOnline = refreshed.isFromLiveNetwork
                                    isRefreshingRates = false
                                    recomputeFromTop(topFieldText)
                                }
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh currency rates",
                        tint = theme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // FIELD 1 (FROM / TOP LARGE FIELD)
        ConverterLargeFieldCard(
            label = "FROM",
            unit = fromUnit,
            valueText = topFieldText,
            isPrimaryAccent = activeEditedSide == 0,
            onValueChange = { newText ->
                activeEditedSide = 0
                topFieldText = newText
                recomputeFromTop(newText)
            },
            onOpenUnitPicker = { pickerTargetSide = 0 },
            testTagPrefix = "converter_from"
        )

        // CENTER ACTION ROW: SWAP, FAVORITE, COPY
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Swap",
                icon = Icons.Default.SwapVert,
                accentFill = theme.accent,
                onClick = {
                    FeedbackHelper.confirmClick(context)
                    val prevFrom = fromUnitCode
                    fromUnitCode = toUnitCode
                    toUnitCode = prevFrom
                    val prevTop = topFieldText
                    topFieldText = bottomFieldText.ifBlank { prevTop }
                    activeEditedSide = 0
                    recomputeFromTop(topFieldText)
                    recordCurrentToHistory()
                },
                modifier = Modifier.weight(1f),
                testTag = "converter_swap_button"
            )

            GlassButton(
                text = if (isCurrentFavorite) "Saved" else "Favorite",
                icon = if (isCurrentFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                onClick = {
                    FeedbackHelper.tick(context)
                    if (isCurrentFavorite) {
                        favorites.removeAll { it.key == currentPair.key }
                        statusBanner = "Removed ${fromUnit.symbol} → ${toUnit.symbol} from favorites"
                    } else {
                        favorites.add(0, currentPair)
                        statusBanner = "Added ${fromUnit.symbol} → ${toUnit.symbol} to favorites"
                    }
                    FrankfurterCurrencyStore.saveFavoritesAndHistory(context, favorites, history)
                },
                modifier = Modifier.weight(1f),
                testTag = "converter_favorite_button"
            )

            GlassButton(
                text = "Copy",
                icon = Icons.Default.ContentCopy,
                onClick = {
                    val formatted = "$topFieldText ${fromUnit.symbol} = $bottomFieldText ${toUnit.symbol}"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Dyne Unit Conversion", formatted))
                    recordCurrentToHistory()
                    statusBanner = "Copied: $formatted"
                },
                modifier = Modifier.weight(1f),
                testTag = "converter_copy_button"
            )
        }

        // FIELD 2 (TO / BOTTOM LARGE FIELD)
        ConverterLargeFieldCard(
            label = "TO",
            unit = toUnit,
            valueText = bottomFieldText,
            isPrimaryAccent = activeEditedSide == 1,
            onValueChange = { newText ->
                activeEditedSide = 1
                bottomFieldText = newText
                recomputeFromBottom(newText)
            },
            onOpenUnitPicker = { pickerTargetSide = 1 },
            testTagPrefix = "converter_to"
        )

        // Formula / Single-unit reference line
        val singleRate = remember(selectedCategory, fromUnit.code, toUnit.code, eurRates.size, currencyFetchedAtMs) {
            UnitCatalog.formatNumber(UnitCatalog.convert(selectedCategory, 1.0, fromUnit, toUnit, eurRates))
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.elevated)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = "1 ${fromUnit.symbol} = $singleRate ${toUnit.symbol}",
                style = type.gaugeSmall,
                color = theme.accent
            )
            Text(
                text = "Log to History",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.bentoSky,
                modifier = Modifier.pressScaleClickable(
                    onClick = {
                        recordCurrentToHistory()
                        statusBanner = "Saved to recent conversions"
                    }
                )
            )
        }

        if (statusBanner != null) {
            Text(
                text = statusBanner ?: "",
                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = theme.accent
            )
        }

        // Reserved banner slot below the conversion result
        com.example.ads.InlineBannerAdSlot(placementTag = "unit_converter_below_result")

        // FAVORITE PAIRS SECTION
        if (favorites.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "FAVORITE PAIRS",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    favorites.forEach { fav ->
                        val cat = ConverterCategory.entries.firstOrNull { it.name == fav.categoryName }
                        if (cat != null) {
                            val catUnits = UnitCatalog.unitsFor(cat)
                            val fU = catUnits.firstOrNull { it.code == fav.fromCode }
                            val tU = catUnits.firstOrNull { it.code == fav.toCode }
                            if (fU != null && tU != null) {
                                val isActive = selectedCategory == cat &&
                                    fromUnitCode == fU.code &&
                                    toUnitCode == tU.code
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isActive) theme.accent.copy(alpha = 0.22f) else theme.elevated)
                                        .border(
                                            1.dp,
                                            if (isActive) theme.accent else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .pressScaleClickable(
                                            onClick = {
                                                selectedCategory = cat
                                                fromUnitCode = fU.code
                                                toUnitCode = tU.code
                                                activeEditedSide = 0
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = theme.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${fU.symbol} → ${tU.symbol}",
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = theme.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // RECENT CONVERSION HISTORY SECTION
        if (history.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(theme.elevated)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recent History",
                            tint = theme.bentoSky,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "RECENT HISTORY",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear History",
                        tint = theme.bentoCoral,
                        modifier = Modifier
                            .size(18.dp)
                            .pressScaleClickable(
                                onClick = {
                                    history.clear()
                                    FrankfurterCurrencyStore.saveFavoritesAndHistory(context, favorites, history)
                                }
                            )
                    )
                }

                history.take(6).forEach { item ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.surface)
                            .pressScaleClickable(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val line = "${item.fromValue} ${item.fromSymbol} = ${item.toValue} ${item.toSymbol}"
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Conversion", line))
                                    statusBanner = "Copied history entry: $line"
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item.categoryName,
                            style = type.caption,
                            color = theme.textSecondary,
                            modifier = Modifier.width(88.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.fromValue} ${item.fromSymbol} = ${item.toValue} ${item.toSymbol}",
                            style = type.gaugeSmall,
                            color = theme.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // UNIT PICKER GLASS SHEET (with 52dp search field & Popular / All split for long lists like Currencies)
    if (pickerTargetSide != null) {
        val side = pickerTargetSide!!
        var unitSearchQuery by rememberSaveable { mutableStateOf("") }
        val popularCodes = remember(selectedCategory) {
            when (selectedCategory) {
                ConverterCategory.CURRENCY -> setOf("USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD", "CHF")
                else -> units.take(4).map { it.code }.toSet()
            }
        }
        val filteredUnits = remember(units, unitSearchQuery) {
            val q = unitSearchQuery.trim()
            if (q.isEmpty()) units else units.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.code.contains(q, ignoreCase = true) ||
                    it.symbol.contains(q, ignoreCase = true)
            }
        }
        val popularUnits = remember(filteredUnits, popularCodes) {
            filteredUnits.filter { it.code in popularCodes }
        }
        val allOtherUnits = remember(filteredUnits, popularCodes) {
            if (units.size >= 8) filteredUnits.filter { it.code !in popularCodes } else emptyList()
        }

        @Composable
        fun UnitOptionRow(unit: UnitDef) {
            val isSelected = if (side == 0) unit.code == fromUnit.code else unit.code == toUnit.code
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) theme.accent.copy(alpha = 0.2f) else theme.elevated)
                    .border(
                        1.dp,
                        if (isSelected) theme.accent else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(16.dp)
                    )
                    .pressScaleClickable(
                        onClick = {
                            if (side == 0) {
                                fromUnitCode = unit.code
                            } else {
                                toUnitCode = unit.code
                            }
                            pickerTargetSide = null
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = unit.name,
                        style = type.cardTitle,
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = unit.code,
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) theme.accent else theme.surface)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = unit.symbol,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) BentoTextBlack else theme.accent
                    )
                }
            }
        }

        GlassSheet(
            onDismissRequest = { pickerTargetSide = null },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (side == 0) "Select Source Unit" else "Select Target Unit",
                    style = type.title,
                    color = theme.textPrimary
                )
                Text(
                    text = "Category: ${selectedCategory.title}",
                    style = type.caption,
                    color = theme.textSecondary
                )

                OutlinedTextField(
                    value = unitSearchQuery,
                    onValueChange = { unitSearchQuery = it },
                    placeholder = {
                        Text(
                            text = if (selectedCategory == ConverterCategory.CURRENCY) "Search currencies" else "Search units",
                            style = type.body,
                            color = theme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search units", tint = theme.accent)
                    },
                    trailingIcon = {
                        if (unitSearchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = theme.textSecondary,
                                modifier = Modifier.pressScaleClickable(onClick = { unitSearchQuery = "" })
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = theme.elevated,
                        unfocusedContainerColor = theme.elevated,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("unit_picker_search_input")
                )

                if (units.size >= 8 && unitSearchQuery.isBlank()) {
                    Text(
                        text = "POPULAR",
                        style = type.section,
                        color = theme.accent
                    )
                    popularUnits.forEach { UnitOptionRow(it) }
                    if (allOtherUnits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ALL",
                            style = type.section,
                            color = theme.accent
                        )
                        allOtherUnits.forEach { UnitOptionRow(it) }
                    }
                } else {
                    filteredUnits.forEach { UnitOptionRow(it) }
                }
            }
        }
    }
}

@Composable
private fun ConverterLargeFieldCard(
    label: String,
    unit: UnitDef,
    valueText: String,
    isPrimaryAccent: Boolean,
    onValueChange: (String) -> Unit,
    onOpenUnitPicker: () -> Unit,
    testTagPrefix: String
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.elevated)
            .border(
                width = if (isPrimaryAccent) 1.5.dp else 1.dp,
                color = if (isPrimaryAccent) theme.accent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )

            // Unit Picker Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.surface)
                    .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .pressScaleClickable(onClick = onOpenUnitPicker)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("${testTagPrefix}_unit_picker")
            ) {
                Text(
                    text = "${unit.name} (${unit.symbol})",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Choose unit",
                    tint = theme.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        OutlinedTextField(
            value = valueText,
            onValueChange = onValueChange,
            textStyle = type.gaugeMedium.copy(color = theme.textPrimary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = {
                Text(
                    text = unit.symbol,
                    style = type.title.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = theme.surface,
                unfocusedContainerColor = theme.surface,
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTagPrefix}_input")
        )
    }
}

@Composable
fun UnitConverterExtrasProScreen(
    forcedModeId: String? = null,
    hazeState: HazeState = remember { HazeState() }
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current

    val customUnits = remember {
        mutableStateListOf(
            UnitDef("furlong", "Furlong", "fur", 201.168),
            UnitDef("cubit", "Royal Cubit", "cbt", 0.524),
            UnitDef("fathom", "Fathom", "ftm", 1.8288)
        )
    }
    var customName by remember { mutableStateOf("") }
    var customSymbol by remember { mutableStateOf("") }
    var customFactorStr by remember { mutableStateOf("1.0") }
    var inputQuantityStr by remember { mutableStateOf("10") }
    var selectedCustomIndex by remember { mutableIntStateOf(0) }
    var csvExportStatus by remember { mutableStateOf<String?>(null) }

    val (loadedFavs, loadedHist) = remember { FrankfurterCurrencyStore.loadFavoritesAndHistory(context) }

    val activeCustom = customUnits.getOrElse(selectedCustomIndex.coerceIn(0, (customUnits.size - 1).coerceAtLeast(0))) {
        UnitDef("custom", "Custom", "u", 1.0)
    }
    val qty = inputQuantityStr.toDoubleOrNull() ?: 0.0
    val convertedBaseValue = qty * activeCustom.factorToBase

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Custom Unit Converter & Builder
        GlassInputCard(title = "Custom Units Builder & Converter (PRO)") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customUnits.size) { idx ->
                    val u = customUnits[idx]
                    val sel = idx == selectedCustomIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) theme.accent else theme.elevated)
                            .pressScaleClickable(onClick = { selectedCustomIndex = idx })
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${u.name} (${u.symbol} = ${u.factorToBase}x base)",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (sel) BentoTextBlack else theme.textPrimary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = inputQuantityStr,
                onValueChange = { inputQuantityStr = it },
                label = { Text("Quantity in ${activeCustom.name} (${activeCustom.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "$inputQuantityStr ${activeCustom.symbol} = ${UnitCatalog.formatNumber(convertedBaseValue)} Base SI Units",
                style = type.cardTitle,
                color = theme.accent
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Unit Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = customSymbol,
                    onValueChange = { customSymbol = it },
                    label = { Text("Symbol") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
                OutlinedTextField(
                    value = customFactorStr,
                    onValueChange = { customFactorStr = it },
                    label = { Text("SI Factor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            GlassButton(
                text = "Save Custom Unit",
                onClick = {
                    val f = customFactorStr.toDoubleOrNull()
                    if (customName.isNotBlank() && f != null && f > 0.0) {
                        val sym = customSymbol.ifBlank { customName.take(3).lowercase() }
                        customUnits.add(UnitDef(sym, customName.trim(), sym, f))
                        selectedCustomIndex = customUnits.lastIndex
                        customName = ""
                        customSymbol = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("save_custom_unit_btn")
            )
        }

        // Favorites & CSV History Export Card
        GlassInputCard(title = "Saved Favorites (${loadedFavs.size}) & CSV History Export") {
            Text(
                text = "Export conversion history and custom unit definitions to a standard RFC-4180 CSV spreadsheet.",
                style = type.caption,
                color = theme.textSecondary
            )
            GlassButton(
                text = "Export Conversion History to CSV",
                onClick = {
                    val csv = buildString {
                        appendLine("Category,FromValue,FromUnit,ToValue,ToUnit,TimestampMs")
                        if (loadedHist.isEmpty()) {
                            appendLine("Length,10,${activeCustom.symbol},${UnitCatalog.formatNumber(convertedBaseValue)},m,${System.currentTimeMillis()}")
                        } else {
                            loadedHist.forEach { h ->
                                appendLine("${h.categoryName},${h.fromValue},${h.fromSymbol},${h.toValue},${h.toSymbol},${h.timestampMs}")
                            }
                        }
                    }
                    val outFile = File(context.cacheDir, "unit_converter_history.csv")
                    runCatching { outFile.writeText(csv) }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Unit Converter CSV", csv))
                    csvExportStatus = "Exported ${loadedHist.size.coerceAtLeast(1)} rows to ${outFile.name} & copied CSV!"
                },
                modifier = Modifier.fillMaxWidth().testTag("export_converter_csv_btn")
            )
            if (csvExportStatus != null) {
                Text(
                    text = csvExportStatus!!,
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.bentoLime
                )
            }
        }

        // Also embed full standard converter below
        UnitConverterInstrumentScreen(
            forcedModeId = forcedModeId,
            hazeState = hazeState
        )
    }
}

