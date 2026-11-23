package com.example.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FeedbackHelper
import com.example.ui.theme.LocalDyneTheme
import java.security.SecureRandom
import java.text.DecimalFormat
import kotlin.math.log2
import kotlin.math.roundToInt

// ================================================================================================
// 1. PASSWORD GENERATOR (FREE: Password, Passphrase, PIN + Entropy Meter)
// ================================================================================================

@Composable
fun PasswordGeneratorScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val view = LocalView.current
    val secureRandom = remember { SecureRandom() }

    val modes = listOf("password" to "Password", "passphrase" to "Passphrase", "pin" to "PIN")
    var activeMode by remember(forcedModeId) { mutableStateOf(forcedModeId ?: "password") }

    var length by remember { mutableIntStateOf(16) }
    var useUpper by remember { mutableStateOf(true) }
    var useLower by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var wordCount by remember { mutableIntStateOf(4) }
    var pinDigits by remember { mutableIntStateOf(6) }

    val wordBank = remember {
        listOf(
            "amber", "anchor", "atlas", "aurora", "beacon", "breeze", "canyon", "cipher",
            "cobalt", "comet", "coral", "cosmos", "crystal", "delta", "dynamo", "eclipse",
            "ember", "falcon", "fjord", "forged", "galaxy", "glacier", "granite", "harbor",
            "helium", "horizon", "indigo", "island", "jasper", "juniper", "krypton", "lagoon",
            "lantern", "legend", "lunar", "magnet", "marble", "matrix", "meadow", "meteor",
            "mirage", "monolith", "nebula", "nectar", "nickel", "nimbus", "obsidian", "odyssey",
            "omega", "onyx", "opal", "orbit", "osprey", "oxygen", "paradox", "pegasus",
            "phoenix", "photon", "pioneer", "plasma", "polaris", "prism", "pulsar", "quartz",
            "quasar", "raptor", "redwood", "ripple", "sapphire", "saturn", "sequoia", "shadow",
            "sierra", "silver", "solstice", "SONAR", "spark", "spectrum", "summit", "tempest",
            "titanium", "topaz", "trident", "tundra", "velvet", "vertex", "Vortex", "voyager",
            "whisper", "zenith", "zephyr", "zircon"
        )
    }

    fun generateSecret(): Pair<String, Double> {
        return when (activeMode) {
            "passphrase" -> {
                val chosen = List(wordCount) { wordBank[secureRandom.nextInt(wordBank.size)].lowercase() }
                val num = secureRandom.nextInt(90) + 10
                val result = chosen.joinToString("-") + "-$num"
                val entropy = wordCount * log2(wordBank.size.toDouble()) + log2(90.0)
                result to entropy
            }
            "pin" -> {
                val result = buildString {
                    repeat(pinDigits) { append(secureRandom.nextInt(10)) }
                }
                result to (pinDigits * log2(10.0))
            }
            else -> {
                var pool = ""
                if (useUpper) pool += "ABCDEFGHJKLMNPQRSTUVWXYZ"
                if (useLower) pool += "abcdefghijkmnopqrstuvwxyz"
                if (useDigits) pool += "23456789"
                if (useSymbols) pool += "!@#$%^&*()-_=+[]{}?"
                if (pool.isEmpty()) pool = "abcdefghijkmnopqrstuvwxyz23456789"
                val result = buildString {
                    repeat(length) { append(pool[secureRandom.nextInt(pool.length)]) }
                }
                val entropy = length * log2(pool.length.toDouble())
                result to entropy
            }
        }
    }

    var generatedPair by remember(activeMode, length, useUpper, useLower, useDigits, useSymbols, wordCount, pinDigits) {
        mutableStateOf(generateSecret())
    }

    val secret = generatedPair.first
    val entropyBits = generatedPair.second
    val strengthLabel = when {
        entropyBits >= 90 -> "Vault Grade (${entropyBits.roundToInt()} bits)"
        entropyBits >= 65 -> "Strong (${entropyBits.roundToInt()} bits)"
        entropyBits >= 45 -> "Moderate (${entropyBits.roundToInt()} bits)"
        else -> "Basic (${entropyBits.roundToInt()} bits)"
    }
    val strengthColor = when {
        entropyBits >= 90 -> Color(0xFF00E676)
        entropyBits >= 65 -> theme.accent
        entropyBits >= 45 -> Color(0xFFFFD54F)
        else -> Color(0xFFFF5252)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (forcedModeId == null) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(modes) { (id, label) ->
                    val selected = activeMode == id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground.copy(alpha = 0.65f),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                FeedbackHelper.hapticTick(view)
                                activeMode = id
                            }
                            .testTag("pwd_mode_$id")
                    ) {
                        Text(
                            text = label,
                            color = if (selected) theme.accent else theme.textPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }

        // Generated Output Display
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = theme.cardBackground.copy(alpha = 0.88f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, strengthColor.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strengthLabel,
                    color = strengthColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = secret,
                    color = theme.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("password_output_text")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            FeedbackHelper.hapticClick(view)
                            generatedPair = generateSecret()
                        },
                        modifier = Modifier.weight(1f).testTag("regenerate_password")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Regenerate")
                    }
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Generated Password", secret))
                            FeedbackHelper.hapticSuccess(view)
                        },
                        modifier = Modifier.weight(1f).testTag("copy_password")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }
                }
            }
        }

        // Options Card
        GlassInputCard(title = "Generator Parameters") {
            when (activeMode) {
                "password" -> {
                    Text("Length: $length characters", fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                    Slider(
                        value = length.toFloat(),
                        onValueChange = { length = it.roundToInt() },
                        valueRange = 8f..48f
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = useUpper, onClick = { useUpper = !useUpper }, label = { Text("A-Z") })
                        FilterChip(selected = useLower, onClick = { useLower = !useLower }, label = { Text("a-z") })
                        FilterChip(selected = useDigits, onClick = { useDigits = !useDigits }, label = { Text("0-9") })
                        FilterChip(selected = useSymbols, onClick = { useSymbols = !useSymbols }, label = { Text("!@#$") })
                    }
                }
                "passphrase" -> {
                    Text("Words: $wordCount words", fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                    Slider(
                        value = wordCount.toFloat(),
                        onValueChange = { wordCount = it.roundToInt() },
                        valueRange = 3f..8f
                    )
                }
                "pin" -> {
                    Text("PIN Digits: $pinDigits digits", fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 6, 8, 10, 12).forEach { d ->
                            FilterChip(
                                selected = pinDigits == d,
                                onClick = { pinDigits = d },
                                label = { Text("$d digits") }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================================================================================================
// 2. COOKING CONVERTER (FREE: cups, grams, ml, tbsp, tsp, oz by ingredient + oven + scaler)
// ================================================================================================

private data class CookingIngredient(
    val name: String,
    val gramsPerCup: Double // 1 US Cup (240 ml) weight in grams
)

@Composable
fun CookingConverterScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current

    val ingredients = remember {
        listOf(
            CookingIngredient("Water / Milk / Liquid", 240.0),
            CookingIngredient("All-Purpose Flour", 120.0),
            CookingIngredient("Granulated Sugar", 200.0),
            CookingIngredient("Brown Sugar (Packed)", 220.0),
            CookingIngredient("Powdered Sugar", 120.0),
            CookingIngredient("Butter", 227.0),
            CookingIngredient("Honey / Maple Syrup", 340.0),
            CookingIngredient("Rolled Oats", 90.0),
            CookingIngredient("Uncooked Rice", 185.0),
            CookingIngredient("Cocoa Powder", 100.0),
            CookingIngredient("Olive / Cooking Oil", 216.0)
        )
    }

    var selectedIngredient by remember { mutableStateOf(ingredients[1]) }
    var inputAmountStr by remember { mutableStateOf("1.5") }
    var inputUnit by remember { mutableStateOf("Cups") }
    var recipeScaleMultiplier by remember { mutableDoubleStateOf(1.0) }
    var ovenCelsiusStr by remember { mutableStateOf("180") }

    val units = listOf("Cups", "Grams (g)", "Milliliters (ml)", "Tablespoons (tbsp)", "Teaspoons (tsp)", "Ounces (oz)")
    val rawAmount = (inputAmountStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0) * recipeScaleMultiplier

    // Normalize to US Cups first
    val cupsValue = when (inputUnit) {
        "Cups" -> rawAmount
        "Grams (g)" -> rawAmount / selectedIngredient.gramsPerCup
        "Milliliters (ml)" -> rawAmount / 240.0
        "Tablespoons (tbsp)" -> rawAmount / 16.0
        "Teaspoons (tsp)" -> rawAmount / 48.0
        "Ounces (oz)" -> (rawAmount * 28.3495) / selectedIngredient.gramsPerCup
        else -> rawAmount
    }

    val grams = cupsValue * selectedIngredient.gramsPerCup
    val ml = cupsValue * 240.0
    val tbsp = cupsValue * 16.0
    val tsp = cupsValue * 48.0
    val oz = grams / 28.3495
    val df = remember { DecimalFormat("#,##0.##") }

    val ovenC = ovenCelsiusStr.toDoubleOrNull() ?: 180.0
    val ovenF = ovenC * 9.0 / 5.0 + 32.0
    val gasMark = ((ovenC - 121.0) / 14.0).roundToInt().coerceIn(1, 9)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "1. Select Ingredient Density") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ingredients) { ing ->
                    FilterChip(
                        selected = selectedIngredient.name == ing.name,
                        onClick = {
                            FeedbackHelper.hapticTick(view)
                            selectedIngredient = ing
                        },
                        label = { Text("${ing.name} (${ing.gramsPerCup.toInt()}g/cup)") }
                    )
                }
            }

            OutlinedTextField(
                value = inputAmountStr,
                onValueChange = { inputAmountStr = it },
                label = { Text("Quantity (${inputUnit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("cooking_amount_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(units) { u ->
                    FilterChip(
                        selected = inputUnit == u,
                        onClick = { inputUnit = u },
                        label = { Text(u) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Recipe Scale:", fontSize = 12.sp, color = theme.textSecondary)
                listOf(0.5 to "1/2x", 1.0 to "1x", 1.5 to "1.5x", 2.0 to "2x", 3.0 to "3x").forEach { (mult, label) ->
                    FilterChip(
                        selected = recipeScaleMultiplier == mult,
                        onClick = { recipeScaleMultiplier = mult },
                        label = { Text(label) }
                    )
                }
            }
        }

        GlassResultCard(
            headlineLabel = "${selectedIngredient.name} (Scale ${recipeScaleMultiplier}x)",
            headlineValue = "${df.format(grams)} g  •  ${df.format(ml)} ml",
            rows = listOf(
                "US Cups" to "${df.format(cupsValue)} cups",
                "Grams (Weight)" to "${df.format(grams)} g",
                "Milliliters (Volume)" to "${df.format(ml)} ml",
                "Tablespoons (tbsp)" to "${df.format(tbsp)} tbsp",
                "Teaspoons (tsp)" to "${df.format(tsp)} tsp",
                "Weight Ounces (oz)" to "${df.format(oz)} oz"
            )
        )

        GlassInputCard(title = "Oven Temperature Quick Reference") {
            OutlinedTextField(
                value = ovenCelsiusStr,
                onValueChange = { ovenCelsiusStr = it },
                label = { Text("Oven Temp (°C)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${df.format(ovenC)} °C  =  ${df.format(ovenF)} °F  =  Fan ${df.format(ovenC - 20)} °C  =  Gas Mark $gasMark",
                color = theme.accent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ================================================================================================
// 3. UNIT PRICE COMPARER (FREE: Compare two or three products)
// ================================================================================================

private data class ProductCompareSlot(
    val label: String,
    var priceStr: String,
    var quantityStr: String,
    var unitFactor: Double, // Normalized to base g or ml or 1 count
    var unitName: String
)

@Composable
fun UnitPriceComparerScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    var compareThree by remember { mutableStateOf(true) }

    var p1Price by remember { mutableStateOf("4.99") }
    var p1Qty by remember { mutableStateOf("500") }
    var p1Unit by remember { mutableStateOf("g / ml") }

    var p2Price by remember { mutableStateOf("8.49") }
    var p2Qty by remember { mutableStateOf("1000") }
    var p2Unit by remember { mutableStateOf("g / ml") }

    var p3Price by remember { mutableStateOf("11.99") }
    var p3Qty by remember { mutableStateOf("1500") }
    var p3Unit by remember { mutableStateOf("g / ml") }

    fun unitMultiplier(unit: String): Double = when (unit) {
        "kg / L" -> 1000.0
        "oz" -> 28.3495
        "lb" -> 453.592
        else -> 1.0
    }

    fun costPer100Base(priceStr: String, qtyStr: String, unit: String): Double? {
        val p = priceStr.toDoubleOrNull() ?: return null
        val q = qtyStr.toDoubleOrNull() ?: return null
        if (p <= 0.0 || q <= 0.0) return null
        val baseUnits = q * unitMultiplier(unit)
        return (p / baseUnits) * 100.0
    }

    val c1 = costPer100Base(p1Price, p1Qty, p1Unit)
    val c2 = costPer100Base(p2Price, p2Qty, p2Unit)
    val c3 = if (compareThree) costPer100Base(p3Price, p3Qty, p3Unit) else null

    val validCosts = listOfNotNull(
        c1?.let { "Product A" to it },
        c2?.let { "Product B" to it },
        c3?.let { "Product C" to it }
    )
    val best = validCosts.minByOrNull { it.second }
    val worst = validCosts.maxByOrNull { it.second }
    val savingsPct = if (best != null && worst != null && worst.second > 0) {
        ((worst.second - best.second) / worst.second) * 100.0
    } else 0.0

    val df = remember { DecimalFormat("#,##0.000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Compare 3 Products", color = theme.textPrimary, fontWeight = FontWeight.Bold)
            Switch(checked = compareThree, onCheckedChange = { compareThree = it })
        }

        ProductSlotCard("Product A", p1Price, { p1Price = it }, p1Qty, { p1Qty = it }, p1Unit, { p1Unit = it }, c1, best?.first == "Product A")
        ProductSlotCard("Product B", p2Price, { p2Price = it }, p2Qty, { p2Qty = it }, p2Unit, { p2Unit = it }, c2, best?.first == "Product B")
        if (compareThree) {
            ProductSlotCard("Product C", p3Price, { p3Price = it }, p3Qty, { p3Qty = it }, p3Unit, { p3Unit = it }, c3, best?.first == "Product C")
        }

        if (best != null) {
            GlassResultCard(
                headlineLabel = "Best Value Winner",
                headlineValue = "${best.first} (Saves ${DecimalFormat("0.0").format(savingsPct)}%)",
                rows = validCosts.map { (name, cost) ->
                    "$name (per 100g / 100ml)" to df.format(cost)
                }
            )
        }
    }
}

@Composable
private fun ProductSlotCard(
    title: String,
    price: String,
    onPrice: (String) -> Unit,
    qty: String,
    onQty: (String) -> Unit,
    unit: String,
    onUnit: (String) -> Unit,
    costPer100: Double?,
    isWinner: Boolean
) {
    val theme = LocalDyneTheme.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = theme.cardBackground.copy(alpha = 0.82f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isWinner) Color(0xFF00E676) else theme.textSecondary.copy(alpha = 0.2f),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (isWinner) {
                    Text("BEST VALUE ★", color = Color(0xFF00E676), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = onPrice,
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = onQty,
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("g / ml", "kg / L", "oz", "lb").forEach { u ->
                    FilterChip(
                        selected = unit == u,
                        onClick = { onUnit(u) },
                        label = { Text(u, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

// ================================================================================================
// 4. FUEL AND TRIP COST CALCULATOR (FREE)
// ================================================================================================

@Composable
fun FuelAndTripCostScreen(forcedModeId: String? = null) {
    var distanceStr by remember { mutableStateOf("420") }
    var efficiencyStr by remember { mutableStateOf("7.2") } // L/100km or km/L
    var isLitersPer100Km by remember { mutableStateOf(true) }
    var fuelPriceStr by remember { mutableStateOf("1.65") }
    var tollsStr by remember { mutableStateOf("12.00") }
    var passengers by remember { mutableIntStateOf(3) }

    val distance = distanceStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val eff = efficiencyStr.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 7.5
    val fuelPrice = fuelPriceStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val tolls = tollsStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val fuelNeededLiters = if (isLitersPer100Km) {
        (distance / 100.0) * eff
    } else {
        distance / eff
    }
    val fuelCost = fuelNeededLiters * fuelPrice
    val totalTripCost = fuelCost + tolls
    val costPerPassenger = totalTripCost / passengers.coerceAtLeast(1)
    val df = remember { DecimalFormat("#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Trip & Vehicle Efficiency") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = distanceStr,
                    onValueChange = { distanceStr = it },
                    label = { Text("Trip Distance (km/mi)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = efficiencyStr,
                    onValueChange = { efficiencyStr = it },
                    label = { Text(if (isLitersPer100Km) "L / 100km" else "km / L (or MPG)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isLitersPer100Km,
                    onClick = { isLitersPer100Km = true },
                    label = { Text("L / 100km") }
                )
                FilterChip(
                    selected = !isLitersPer100Km,
                    onClick = { isLitersPer100Km = false },
                    label = { Text("km/L or MPG") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fuelPriceStr,
                    onValueChange = { fuelPriceStr = it },
                    label = { Text("Fuel Price / Unit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = tollsStr,
                    onValueChange = { tollsStr = it },
                    label = { Text("Tolls & Parking") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Passengers Splitting Cost: $passengers", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { if (passengers > 1) passengers-- }) { Text("-") }
                    OutlinedButton(onClick = { passengers++ }) { Text("+") }
                }
            }
        }

        GlassResultCard(
            headlineLabel = "Cost Per Person ($passengers Riders)",
            headlineValue = df.format(costPerPassenger),
            rows = listOf(
                "Total Trip Cost (Fuel + Tolls)" to df.format(totalTripCost),
                "Fuel Cost Only" to df.format(fuelCost),
                "Fuel Volume Required" to "${df.format(fuelNeededLiters)} L / gal",
                "Cost per km/mi" to if (distance > 0) df.format(totalTripCost / distance) else "0.00"
            )
        )
    }
}

// ================================================================================================
// 5. TEXT TOOLS (FREE: Word and character counter, case converter, Morse translator)
// ================================================================================================

@Composable
fun TextToolsScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val view = LocalView.current

    val modes = listOf(
        "counter" to "Word & Char Counter",
        "case" to "Case Converter",
        "morse" to "Morse Translator"
    )
    var activeMode by remember(forcedModeId) { mutableStateOf(forcedModeId ?: "counter") }
    var inputText by remember { mutableStateOf("All-In-One utility toolkit ready for offline field operations.") }

    val morseMap = remember {
        mapOf(
            'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
            'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
            'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
            'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
            'Y' to "-.--", 'Z' to "--..",
            '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
            '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
        )
    }
    val reverseMorseMap = remember { morseMap.entries.associate { (k, v) -> v to k } }

    fun encodeMorse(text: String): String =
        text.uppercase().map { ch ->
            if (ch == ' ') "/" else morseMap[ch] ?: ""
        }.filter { it.isNotEmpty() }.joinToString(" ")

    fun decodeMorse(morse: String): String =
        morse.trim().split(" ").map { token ->
            if (token == "/") ' ' else reverseMorseMap[token] ?: '?'
        }.joinToString("")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (forcedModeId == null) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(modes) { (id, label) ->
                    val selected = activeMode == id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground.copy(alpha = 0.65f),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                FeedbackHelper.hapticTick(view)
                                activeMode = id
                            }
                            .testTag("text_mode_$id")
                    ) {
                        Text(
                            text = label,
                            color = if (selected) theme.accent else theme.textPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }

        GlassInputCard(title = "Input Text") {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Enter text or Morse code (.- / -...)") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth().testTag("text_tools_input")
            )
        }

        when (activeMode) {
            "counter" -> {
                val words = inputText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                val chars = inputText.length
                val charsNoSpaces = inputText.count { !it.isWhitespace() }
                val sentences = inputText.split(Regex("[.!?]+")).count { it.isNotBlank() }
                val lines = if (inputText.isEmpty()) 0 else inputText.lines().size
                val readingSec = ((words.size / 200.0) * 60).roundToInt()

                GlassResultCard(
                    headlineLabel = "Word & Character Breakdown",
                    headlineValue = "${words.size} Words  •  $chars Chars",
                    rows = listOf(
                        "Characters (without spaces)" to "$charsNoSpaces",
                        "Sentences" to "$sentences",
                        "Lines" to "$lines",
                        "Estimated Reading Time" to "${readingSec}s"
                    )
                )
            }
            "case" -> {
                val words = inputText.trim().split(Regex("[\\s_-]+")).filter { it.isNotEmpty() }
                val titleCase = words.joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
                val camelCase = words.mapIndexed { idx, w ->
                    if (idx == 0) w.lowercase() else w.lowercase().replaceFirstChar { it.uppercase() }
                }.joinToString("")
                val snakeCase = words.joinToString("_") { it.lowercase() }

                GlassInputCard(title = "One-Tap Case Transformations") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { inputText = inputText.uppercase() }, modifier = Modifier.weight(1f)) { Text("UPPERCASE") }
                        OutlinedButton(onClick = { inputText = inputText.lowercase() }, modifier = Modifier.weight(1f)) { Text("lowercase") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { inputText = titleCase }, modifier = Modifier.weight(1f)) { Text("Title Case") }
                        OutlinedButton(onClick = { inputText = snakeCase }, modifier = Modifier.weight(1f)) { Text("snake_case") }
                        OutlinedButton(onClick = { inputText = camelCase }, modifier = Modifier.weight(1f)) { Text("camelCase") }
                    }
                }
            }
            "morse" -> {
                val isMorseInput = inputText.trim().all { it == '.' || it == '-' || it == ' ' || it == '/' }
                val translated = if (isMorseInput && inputText.isNotBlank()) {
                    decodeMorse(inputText)
                } else {
                    encodeMorse(inputText)
                }

                GlassResultCard(
                    headlineLabel = if (isMorseInput) "Decoded Plain Text" else "Encoded International Morse",
                    headlineValue = translated.ifEmpty { "..." },
                    rows = listOf(
                        "Direction" to if (isMorseInput) "Morse -> Text" else "Text -> Morse",
                        "SOS Signal Reference" to "... --- ..."
                    )
                )
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Morse Translation", translated))
                        FeedbackHelper.hapticSuccess(view)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Translated Output")
                }
            }
        }
    }
}

// ================================================================================================
// 6. NUMBER TOOLS (FREE: Base converter Bin/Oct/Dec/Hex + Two-way Roman numerals)
// ================================================================================================

@Composable
fun NumberToolsScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current

    val modes = listOf("base" to "Base Converter", "roman" to "Roman Numerals")
    var activeMode by remember(forcedModeId) { mutableStateOf(forcedModeId ?: "base") }

    var baseInputStr by remember { mutableStateOf("255") }
    var sourceRadix by remember { mutableIntStateOf(10) }

    var romanOrNumberStr by remember { mutableStateOf("2026") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (forcedModeId == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { (id, label) ->
                    val selected = activeMode == id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground.copy(alpha = 0.65f),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                FeedbackHelper.hapticTick(view)
                                activeMode = id
                            }
                            .testTag("number_mode_$id")
                    ) {
                        Text(
                            text = label,
                            color = if (selected) theme.accent else theme.textPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        if (activeMode == "base") {
            val parsedValue = runCatching { baseInputStr.trim().toLong(sourceRadix) }.getOrNull()

            GlassInputCard(title = "Radix Base Converter (Bin / Oct / Dec / Hex)") {
                OutlinedTextField(
                    value = baseInputStr,
                    onValueChange = { baseInputStr = it.uppercase() },
                    label = { Text("Input Value (Base $sourceRadix)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("base_converter_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2 to "BIN (2)", 8 to "OCT (8)", 10 to "DEC (10)", 16 to "HEX (16)").forEach { (radix, label) ->
                        FilterChip(
                            selected = sourceRadix == radix,
                            onClick = { sourceRadix = radix },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (parsedValue != null) {
                GlassResultCard(
                    headlineLabel = "HEX (Base 16) / DEC (Base 10)",
                    headlineValue = "0x${parsedValue.toString(16).uppercase()}  •  $parsedValue",
                    rows = listOf(
                        "Binary (Base 2)" to parsedValue.toString(2),
                        "Octal (Base 8)" to parsedValue.toString(8),
                        "Decimal (Base 10)" to parsedValue.toString(10),
                        "Hexadecimal (Base 16)" to parsedValue.toString(16).uppercase(),
                        "Bit Count (Set 1-bits)" to "${java.lang.Long.bitCount(parsedValue)} bits"
                    )
                )
            }
        } else {
            val trimmed = romanOrNumberStr.trim().uppercase()
            val asInt = trimmed.toIntOrNull()
            val romanOutput = if (asInt != null && asInt in 1..3999) {
                intToRoman(asInt)
            } else if (trimmed.isNotEmpty()) {
                romanToInt(trimmed)?.toString() ?: "Invalid (1 - 3999)"
            } else ""

            GlassInputCard(title = "Two-Way Roman Numeral Converter (1 – 3999)") {
                OutlinedTextField(
                    value = romanOrNumberStr,
                    onValueChange = { romanOrNumberStr = it },
                    label = { Text("Enter Integer (e.g. 2026) or Roman (e.g. MMXXVI)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("roman_converter_input")
                )
            }

            GlassResultCard(
                headlineLabel = if (asInt != null) "Roman Numeral Representation" else "Arabic Integer Value",
                headlineValue = romanOutput,
                rows = listOf(
                    "M = 1000, D = 500, C = 100" to "L = 50, X = 10, V = 5, I = 1",
                    "Subtractive Pairs" to "CM=900, CD=400, XC=90, XL=40, IX=9, IV=4"
                )
            )
        }
    }
}

fun intToRoman(num: Int): String {
    if (num !in 1..3999) return "Out of Range"
    val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
    var remaining = num
    return buildString {
        for (i in values.indices) {
            while (remaining >= values[i]) {
                remaining -= values[i]
                append(symbols[i])
            }
        }
    }
}

fun romanToInt(s: String): Int? {
    val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
    if (s.isEmpty() || s.any { it !in map }) return null
    var total = 0
    for (i in s.indices) {
        val current = map[s[i]] ?: return null
        val next = if (i + 1 < s.length) map[s[i + 1]] ?: 0 else 0
        if (current < next) total -= current else total += current
    }
    return total
}
