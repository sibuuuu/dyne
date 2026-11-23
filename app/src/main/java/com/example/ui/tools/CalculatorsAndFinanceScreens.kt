package com.example.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.ProBadge
import com.example.ui.theme.LocalDyneTheme
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.*

// ================================================================================================
// 1. CALCULATORS SUITE (FREE: Basic, EMI/Loan, Percent, Discount, GST/VAT, Tip-split, Date, Age;
//                       PREMIUM: Scientific Calculator with History)
// ================================================================================================

data class CalcHistoryEntry(
    val expression: String,
    val result: String,
    val timestampLabel: String
)

@Composable
fun CalculatorsSuiteScreen(
    forcedModeId: String? = null,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("dyne_calculators_prefs", Context.MODE_PRIVATE) }

    val modes = remember {
        listOf(
            "basic" to "Basic",
            "emi" to "EMI / Loan",
            "percent" to "Percent",
            "discount" to "Discount",
            "gst" to "GST / VAT",
            "tip" to "Tip Split",
            "date" to "Date Diff",
            "age" to "Age",
            "scientific" to "Scientific PRO"
        )
    }

    var activeMode by remember(forcedModeId) {
        mutableStateOf(
            when (forcedModeId) {
                "history" -> "scientific"
                null -> "basic"
                else -> forcedModeId
            }
        )
    }

    val featureGate = LocalFeatureGate.current
    val scientificUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.SCIENTIFIC_DIY_CALCULATORS) == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode switcher pills (hidden when opened as a dedicated single-feature screen)
        if (forcedModeId == null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(modes) { (id, label) ->
                    val selected = activeMode == id
                    val isProTab = id == "scientific"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.elevated.copy(alpha = 0.65f),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                FeedbackHelper.hapticTick(context)
                                activeMode = id
                            }
                            .testTag("calc_mode_$id")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) theme.accent else theme.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            if (isProTab && !scientificUnlocked) {
                                ProBadge(label = "DEMO")
                            }
                        }
                    }
                }
            }
        }

        // Main content box
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeMode) {
                "basic" -> BasicAndScientificKeypadView(
                    isScientific = false,
                    isReadOnlyDemo = false,
                    prefs = prefs,
                    onRequestScientificPro = {
                        activeMode = "scientific"
                    }
                )
                "scientific" -> {
                    BasicAndScientificKeypadView(
                        isScientific = true,
                        isReadOnlyDemo = !scientificUnlocked,
                        prefs = prefs,
                        onRequestScientificPro = onOpenPaywall
                    )
                }
                "emi" -> EmiLoanCalculatorView()
                "percent" -> PercentCalculatorView()
                "discount" -> DiscountCalculatorView()
                "gst" -> GstVatCalculatorView()
                "tip" -> TipSplitCalculatorView()
                "date" -> DateDifferenceCalculatorView()
                "age" -> AgeCalculatorView()
                else -> BasicAndScientificKeypadView(
                    isScientific = false,
                    isReadOnlyDemo = false,
                    prefs = prefs,
                    onRequestScientificPro = onOpenPaywall
                )
            }
        }
    }
}

@Composable
private fun LockedFeatureInlineCard(
    title: String,
    subtitle: String,
    onUnlock: () -> Unit
) {
    val theme = LocalDyneTheme.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = theme.elevated.copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProBadge()
            Text(
                text = title,
                color = theme.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = theme.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD54F),
                    contentColor = Color(0xFF1A1500)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("unlock_inline_button")
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock for 1 Hour or Go Premium", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BasicAndScientificKeypadView(
    isScientific: Boolean,
    isReadOnlyDemo: Boolean = false,
    prefs: android.content.SharedPreferences,
    onRequestScientificPro: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current
    val context = LocalContext.current

    var expression by remember(isReadOnlyDemo) {
        mutableStateOf(if (isReadOnlyDemo) "sin(45)+cos(45)" else "")
    }
    var displayResult by remember(isReadOnlyDemo) {
        mutableStateOf(if (isReadOnlyDemo) "1.41421356" else "0")
    }
    var isDegrees by remember { mutableStateOf(true) }
    var memoryValue by remember(isReadOnlyDemo) { mutableDoubleStateOf(if (isReadOnlyDemo) 3.14159265 else 0.0) }
    var showHistorySheet by remember(isReadOnlyDemo) { mutableStateOf(isReadOnlyDemo) }

    val historyList = remember(isReadOnlyDemo) {
        mutableStateListOf<CalcHistoryEntry>().apply {
            if (isReadOnlyDemo) {
                add(CalcHistoryEntry("sin(45)+cos(45)", "1.41421356", "DEG • Sample"))
                add(CalcHistoryEntry("log(2500)*√(144)", "40.77528", "DEG • Sample"))
                add(CalcHistoryEntry("2^10 + 5!", "1144", "DEG • Sample"))
            } else {
                val saved = prefs.getString("sci_calc_history_v1", "") ?: ""
                if (saved.isNotBlank()) {
                    saved.split("\n").forEach { line ->
                        val parts = line.split("|||")
                        if (parts.size == 3) {
                            add(CalcHistoryEntry(parts[0], parts[1], parts[2]))
                        }
                    }
                }
            }
        }
    }

    fun persistHistory() {
        if (isReadOnlyDemo) return
        val raw = historyList.take(40).joinToString("\n") { "${it.expression}|||${it.result}|||${it.timestampLabel}" }
        prefs.edit().putString("sci_calc_history_v1", raw).apply()
    }

    fun appendToken(token: String) {
        FeedbackHelper.hapticTick(context)
        if (isReadOnlyDemo) {
            onRequestScientificPro()
            return
        }
        expression += token
        val preview = evaluateMathExpression(expression, isDegrees)
        if (preview != null && !preview.isNaN() && !preview.isInfinite()) {
            displayResult = formatCalcNumber(preview)
        }
    }

    fun evaluateNow() {
        FeedbackHelper.hapticTick(context)
        if (isReadOnlyDemo) {
            onRequestScientificPro()
            return
        }
        if (expression.isBlank()) return
        val value = evaluateMathExpression(expression, isDegrees)
        if (value != null && !value.isNaN() && !value.isInfinite()) {
            val formatted = formatCalcNumber(value)
            val entry = CalcHistoryEntry(
                expression = expression,
                result = formatted,
                timestampLabel = if (isDegrees) "DEG" else "RAD"
            )
            historyList.add(0, entry)
            persistHistory()
            displayResult = formatted
            expression = formatted
        } else {
            displayResult = "Error"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isReadOnlyDemo) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFD54F).copy(alpha = 0.16f))
                    .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .clickable { onRequestScientificPro() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("sci_calc_demo_banner")
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "READ-ONLY DEMO • Sample Data",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Inspect sample expressions & tape • Tap any key or Unlock to calculate",
                        color = theme.textSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFD54F)
                ) {
                    Text(
                        text = "Unlock",
                        color = Color(0xFF1A1500),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        // Glass display readout
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = theme.elevated.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isScientific) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = theme.accent.copy(alpha = 0.18f),
                                modifier = Modifier.clickable {
                                    FeedbackHelper.hapticTick(context)
                                    isDegrees = !isDegrees
                                }
                            ) {
                                Text(
                                    text = if (isDegrees) "DEG" else "RAD",
                                    color = theme.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (memoryValue != 0.0) {
                                Text(
                                    text = "M=${formatCalcNumber(memoryValue)}",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = "STANDARD",
                                color = theme.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isScientific) {
                            IconButton(
                                onClick = { showHistorySheet = !showHistorySheet },
                                modifier = Modifier.size(32.dp).testTag("calc_history_toggle")
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "Calculation History",
                                    tint = if (showHistorySheet) theme.accent else theme.textSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Calculator Result", displayResult))
                                FeedbackHelper.hapticTick(context)
                            },
                            modifier = Modifier.size(32.dp).testTag("calc_copy_result")
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Result",
                                tint = theme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (expression.isEmpty()) "0" else expression,
                    color = theme.textSecondary,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = displayResult,
                    color = theme.textPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_display_result")
                )
            }
        }

        // Collapsible History Drawer for Scientific Calculator
        AnimatedVisibility(visible = isScientific && showHistorySheet) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = theme.elevated.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 170.dp)
                    .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History Tape (${historyList.size})",
                            color = theme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (historyList.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    historyList.clear()
                                    persistHistory()
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (historyList.isEmpty()) {
                        Text(
                            text = "No calculations saved yet.",
                            color = theme.textSecondary,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(historyList) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(theme.surface.copy(alpha = 0.45f))
                                        .clickable {
                                            expression = item.result
                                            displayResult = item.result
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.expression} =",
                                        color = theme.textSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.result,
                                        color = theme.accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scientific function rows
        if (isScientific) {
            val sciRows = listOf(
                listOf("sin(", "cos(", "tan(", "ln(", "log("),
                listOf("√(", "^", "π", "e", "!"),
                listOf("(", ")", "MC", "M+", "MR")
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sciRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { key ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = theme.elevated.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .border(1.dp, theme.accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        when (key) {
                                            "MC" -> {
                                                FeedbackHelper.hapticTick(context)
                                                memoryValue = 0.0
                                            }
                                            "M+" -> {
                                                FeedbackHelper.hapticTick(context)
                                                memoryValue += (displayResult.toDoubleOrNull() ?: 0.0)
                                            }
                                            "MR" -> {
                                                appendToken(formatCalcNumber(memoryValue))
                                            }
                                            else -> appendToken(key)
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key.removeSuffix("("),
                                        color = theme.accent,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Standard keypad
        val mainRows = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mainRows.forEach { row ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val isOperator = key in listOf("÷", "×", "-", "+", "=")
                        val isAction = key in listOf("AC", "⌫", "%")
                        val bgColor = when {
                            key == "=" -> theme.accent
                            isOperator -> theme.accent.copy(alpha = 0.2f)
                            isAction -> theme.elevated.copy(alpha = 0.9f)
                            else -> theme.elevated.copy(alpha = 0.65f)
                        }
                        val fgColor = when {
                            key == "=" -> Color(0xFF08121E)
                            isOperator -> theme.accent
                            key == "AC" -> Color(0xFFFF5252)
                            else -> theme.textPrimary
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = bgColor,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(
                                    1.dp,
                                    if (isOperator) theme.accent.copy(alpha = 0.4f) else theme.textSecondary.copy(alpha = 0.15f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    when (key) {
                                        "AC" -> {
                                            FeedbackHelper.hapticTick(context)
                                            expression = ""
                                            displayResult = "0"
                                        }
                                        "⌫" -> {
                                            FeedbackHelper.hapticTick(context)
                                            if (expression.isNotEmpty()) {
                                                expression = expression.dropLast(1)
                                                val preview = evaluateMathExpression(expression, isDegrees)
                                                displayResult = if (preview != null && !preview.isNaN()) {
                                                    formatCalcNumber(preview)
                                                } else if (expression.isEmpty()) {
                                                    "0"
                                                } else {
                                                    displayResult
                                                }
                                            }
                                        }
                                        "±" -> {
                                            FeedbackHelper.hapticTick(context)
                                            val current = displayResult.toDoubleOrNull()
                                            if (current != null && current != 0.0) {
                                                val flipped = formatCalcNumber(-current)
                                                expression = flipped
                                                displayResult = flipped
                                            }
                                        }
                                        "=" -> evaluateNow()
                                        else -> appendToken(key)
                                    }
                                }
                                .testTag("calc_key_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    color = fgColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Safe recursive descent math evaluator supporting +, -, ×, ÷, %, ^, !, parentheses, sin, cos, tan, ln, log, √, π, e
fun evaluateMathExpression(raw: String, isDegrees: Boolean = true): Double? {
    if (raw.isBlank()) return null
    val expr = raw
        .replace("×", "*")
        .replace("÷", "/")
        .replace("π", Math.PI.toString())
        .replace("e", Math.E.toString())
        .replace(" ", "")

    return try {
        object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expr.length) expr[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) return Double.NaN
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> {
                            val d = parseFactor()
                            x = if (d == 0.0) Double.NaN else x / d
                        }
                        eat('%'.code) -> {
                            // If followed by a number, modulo; otherwise percentage (/100)
                            x /= 100.0
                        }
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code) // Auto-close if missing at end
                } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code) {
                    while (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code) nextChar()
                    val func = expr.substring(startPos, pos)
                    x = parseFactor()
                    val rad = if (isDegrees) Math.toRadians(x) else x
                    x = when (func) {
                        "√", "sqrt" -> sqrt(x)
                        "sin" -> sin(rad)
                        "cos" -> cos(rad)
                        "tan" -> tan(rad)
                        "ln" -> ln(x)
                        "log" -> log10(x)
                        else -> Double.NaN
                    }
                } else {
                    return Double.NaN
                }

                if (eat('^'.code)) x = x.pow(parseFactor())
                while (eat('!'.code)) {
                    val n = x.roundToInt().coerceIn(0, 170)
                    var fact = 1.0
                    for (i in 2..n) fact *= i.toDouble()
                    x = fact
                }
                return x
            }
        }.parse()
    } catch (_: Exception) {
        null
    }
}

fun formatCalcNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Error"
    val asLong = value.toLong()
    return if (abs(value - asLong) < 1e-10) {
        asLong.toString()
    } else {
        DecimalFormat("#.########").format(value)
    }
}

// ================================================================================================
// FREE SUB-CALCULATORS: EMI/Loan, Percent, Discount, GST/VAT, Tip-Split, Date Diff, Age
// ================================================================================================

@Composable
private fun EmiLoanCalculatorView() {
    val theme = LocalDyneTheme.current
    var principalStr by remember { mutableStateOf("25000") }
    var annualRateStr by remember { mutableStateOf("7.5") }
    var tenureMonthsStr by remember { mutableStateOf("60") }

    val principal = principalStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val annualRate = annualRateStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val months = tenureMonthsStr.toIntOrNull()?.coerceAtLeast(1) ?: 1

    val monthlyRate = annualRate / 12.0 / 100.0
    val emi = if (monthlyRate == 0.0) {
        principal / months
    } else {
        val factor = (1.0 + monthlyRate).pow(months)
        principal * monthlyRate * factor / (factor - 1.0)
    }
    val totalPayment = emi * months
    val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)
    val df = remember { DecimalFormat("#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "EMI / Loan Parameters") {
            OutlinedTextField(
                value = principalStr,
                onValueChange = { principalStr = it },
                label = { Text("Loan Principal Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = annualRateStr,
                    onValueChange = { annualRateStr = it },
                    label = { Text("Interest (% / yr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = tenureMonthsStr,
                    onValueChange = { tenureMonthsStr = it },
                    label = { Text("Tenure (Months)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        GlassResultCard(
            headlineLabel = "Monthly EMI",
            headlineValue = df.format(emi),
            rows = listOf(
                "Principal Amount" to df.format(principal),
                "Total Interest Payable" to df.format(totalInterest),
                "Total Payment (Principal + Interest)" to df.format(totalPayment),
                "Interest Share" to if (totalPayment > 0) "${DecimalFormat("0.0").format(totalInterest / totalPayment * 100)}%" else "0%"
            )
        )
    }
}

@Composable
private fun PercentCalculatorView() {
    var xStr by remember { mutableStateOf("20") }
    var yStr by remember { mutableStateOf("250") }

    val x = xStr.toDoubleOrNull() ?: 0.0
    val y = yStr.toDoubleOrNull() ?: 0.0
    val df = remember { DecimalFormat("#,##0.##") }

    val xPercentOfY = (x / 100.0) * y
    val xIsWhatPercentOfY = if (y != 0.0) (x / y) * 100.0 else 0.0
    val pctChangeFromXToY = if (x != 0.0) ((y - x) / abs(x)) * 100.0 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Percentage Calculator") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = xStr,
                    onValueChange = { xStr = it },
                    label = { Text("Value X") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = yStr,
                    onValueChange = { yStr = it },
                    label = { Text("Value Y") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        GlassResultCard(
            headlineLabel = "${df.format(x)}% of ${df.format(y)}",
            headlineValue = df.format(xPercentOfY),
            rows = listOf(
                "${df.format(x)} is what % of ${df.format(y)}?" to "${df.format(xIsWhatPercentOfY)}%",
                "% Change from ${df.format(x)} to ${df.format(y)}" to "${if (pctChangeFromXToY >= 0) "+" else ""}${df.format(pctChangeFromXToY)}%",
                "${df.format(y)} increased by ${df.format(x)}%" to df.format(y * (1 + x / 100.0)),
                "${df.format(y)} decreased by ${df.format(x)}%" to df.format(y * (1 - x / 100.0))
            )
        )
    }
}

@Composable
private fun DiscountCalculatorView() {
    var priceStr by remember { mutableStateOf("120") }
    var discountPctStr by remember { mutableStateOf("25") }
    var extraOffPctStr by remember { mutableStateOf("10") }
    var salesTaxPctStr by remember { mutableStateOf("0") }

    val price = priceStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val disc1 = (discountPctStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val disc2 = (extraOffPctStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val tax = (salesTaxPctStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)

    val afterFirst = price * (1.0 - disc1 / 100.0)
    val discountedSubTotal = afterFirst * (1.0 - disc2 / 100.0)
    val totalSaved = price - discountedSubTotal
    val taxAmount = discountedSubTotal * (tax / 100.0)
    val finalPayable = discountedSubTotal + taxAmount
    val effectiveDiscountPct = if (price > 0) (totalSaved / price) * 100.0 else 0.0
    val df = remember { DecimalFormat("#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Discount & Stacked Promo") {
            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("Original Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = discountPctStr,
                    onValueChange = { discountPctStr = it },
                    label = { Text("Discount %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = extraOffPctStr,
                    onValueChange = { extraOffPctStr = it },
                    label = { Text("Extra % Off") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = salesTaxPctStr,
                    onValueChange = { salesTaxPctStr = it },
                    label = { Text("Tax %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        GlassResultCard(
            headlineLabel = "Final Price",
            headlineValue = df.format(finalPayable),
            rows = listOf(
                "You Save" to df.format(totalSaved),
                "Effective Discount" to "${DecimalFormat("0.0").format(effectiveDiscountPct)}%",
                "Subtotal After Discounts" to df.format(discountedSubTotal),
                "Tax Added" to df.format(taxAmount)
            )
        )
    }
}

@Composable
private fun GstVatCalculatorView() {
    val theme = LocalDyneTheme.current
    var amountStr by remember { mutableStateOf("1000") }
    var ratePct by remember { mutableDoubleStateOf(18.0) }
    var isInclusive by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val netAmount: Double
    val taxAmount: Double
    val grossAmount: Double

    if (isInclusive) {
        grossAmount = amount
        netAmount = amount / (1.0 + ratePct / 100.0)
        taxAmount = grossAmount - netAmount
    } else {
        netAmount = amount
        taxAmount = amount * (ratePct / 100.0)
        grossAmount = netAmount + taxAmount
    }
    val df = remember { DecimalFormat("#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "GST / VAT Calculator") {
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text(if (isInclusive) "Gross Amount (Tax Inclusive)" else "Net Amount (Tax Exclusive)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5.0, 12.0, 18.0, 20.0, 28.0).forEach { slab ->
                    FilterChip(
                        selected = ratePct == slab,
                        onClick = { ratePct = slab },
                        label = { Text("${slab.toInt()}%") }
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mode: ${if (isInclusive) "Remove GST/VAT (Inclusive)" else "Add GST/VAT (Exclusive)"}", color = theme.textPrimary, fontSize = 13.sp)
                Switch(checked = isInclusive, onCheckedChange = { isInclusive = it })
            }
        }

        GlassResultCard(
            headlineLabel = "Gross Total",
            headlineValue = df.format(grossAmount),
            rows = listOf(
                "Net Pre-Tax Amount" to df.format(netAmount),
                "Total GST / VAT (${ratePct.toInt()}%)" to df.format(taxAmount),
                "CGST / SGST Split (${ratePct / 2}% each)" to "${df.format(taxAmount / 2.0)} + ${df.format(taxAmount / 2.0)}"
            )
        )
    }
}

@Composable
private fun TipSplitCalculatorView() {
    var billStr by remember { mutableStateOf("86.50") }
    var tipPct by remember { mutableFloatStateOf(18f) }
    var people by remember { mutableIntStateOf(3) }

    val bill = billStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val tipAmount = bill * (tipPct / 100.0)
    val total = bill + tipAmount
    val perPerson = total / people.coerceAtLeast(1)
    val df = remember { DecimalFormat("#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Quick Tip & Split") {
            OutlinedTextField(
                value = billStr,
                onValueChange = { billStr = it },
                label = { Text("Bill Subtotal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Tip Percentage: ${tipPct.roundToInt()}%", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Slider(
                value = tipPct,
                onValueChange = { tipPct = it },
                valueRange = 0f..35f
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Split Between People: $people", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (people > 1) people-- }) { Text("-") }
                    OutlinedButton(onClick = { people++ }) { Text("+") }
                }
            }
        }

        GlassResultCard(
            headlineLabel = "Each Person Pays",
            headlineValue = df.format(perPerson),
            rows = listOf(
                "Tip Amount (${tipPct.roundToInt()}%)" to df.format(tipAmount),
                "Grand Total" to df.format(total),
                "Tip per Person" to df.format(tipAmount / people.coerceAtLeast(1))
            )
        )
    }
}

@Composable
private fun DateDifferenceCalculatorView() {
    var startStr by remember { mutableStateOf(LocalDate.now().minusDays(90).toString()) }
    var endStr by remember { mutableStateOf(LocalDate.now().toString()) }

    val parsedStart = runCatching { LocalDate.parse(startStr.trim()) }.getOrNull()
    val parsedEnd = runCatching { LocalDate.parse(endStr.trim()) }.getOrNull()

    val period = if (parsedStart != null && parsedEnd != null) {
        Period.between(parsedStart, parsedEnd)
    } else null
    val totalDays = if (parsedStart != null && parsedEnd != null) {
        abs(ChronoUnit.DAYS.between(parsedStart, parsedEnd))
    } else 0L
    val totalWeeks = totalDays / 7
    val remDays = totalDays % 7

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Date Difference (YYYY-MM-DD)") {
            OutlinedTextField(
                value = startStr,
                onValueChange = { startStr = it },
                label = { Text("Start Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = endStr,
                onValueChange = { endStr = it },
                label = { Text("End Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (period != null) {
            GlassResultCard(
                headlineLabel = "Calendar Span",
                headlineValue = "${abs(period.years)}y ${abs(period.months)}m ${abs(period.days)}d",
                rows = listOf(
                    "Total Days" to "$totalDays days",
                    "Weeks + Days" to "$totalWeeks weeks, $remDays days",
                    "Total Hours" to "${totalDays * 24} hours",
                    "Approximate Workdays" to "${(totalDays * 5) / 7} days"
                )
            )
        }
    }
}

@Composable
private fun AgeCalculatorView() {
    var birthDateStr by remember { mutableStateOf("1996-05-15") }
    val birthDate = runCatching { LocalDate.parse(birthDateStr.trim()) }.getOrNull()
    val today = LocalDate.now()

    val agePeriod = if (birthDate != null && !birthDate.isAfter(today)) {
        Period.between(birthDate, today)
    } else null

    val totalDaysLived = if (birthDate != null && !birthDate.isAfter(today)) {
        ChronoUnit.DAYS.between(birthDate, today)
    } else 0L

    val nextBirthdayDays = if (birthDate != null) {
        var nextBday = birthDate.withYear(today.year)
        if (!nextBday.isAfter(today)) nextBday = nextBday.plusYears(1)
        ChronoUnit.DAYS.between(today, nextBday)
    } else 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Exact Age Calculator") {
            OutlinedTextField(
                value = birthDateStr,
                onValueChange = { birthDateStr = it },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (agePeriod != null) {
            GlassResultCard(
                headlineLabel = "Current Age",
                headlineValue = "${agePeriod.years} yrs ${agePeriod.months} mos ${agePeriod.days} days",
                rows = listOf(
                    "Next Birthday In" to "$nextBirthdayDays days",
                    "Total Months Lived" to "${agePeriod.years * 12 + agePeriod.months} months",
                    "Total Weeks Lived" to "${totalDaysLived / 7} weeks",
                    "Total Days Lived" to "$totalDaysLived days"
                )
            )
        }
    }
}

// ================================================================================================
// 2. FINANCE PACK PRO (Compound Interest, SIP, Loan Comparison, Savings Goal, Currency Alerts)
// ================================================================================================

@Composable
fun FinancePackScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val modes = remember {
        listOf(
            "compound" to "Compound",
            "sip" to "SIP",
            "loan_compare" to "Loan Compare",
            "savings_goal" to "Savings Goal",
            "currency_alerts" to "Currency Alerts"
        )
    }
    var activeMode by remember(forcedModeId) { mutableStateOf(forcedModeId ?: "compound") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (forcedModeId == null) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(modes) { (id, label) ->
                    val selected = activeMode == id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.elevated.copy(alpha = 0.65f),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                FeedbackHelper.hapticTick(context)
                                activeMode = id
                            }
                            .testTag("finance_mode_$id")
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

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeMode) {
                "compound" -> CompoundInterestView()
                "sip" -> SipCalculatorView()
                "loan_compare" -> LoanComparisonView()
                "savings_goal" -> SavingsGoalView()
                "currency_alerts" -> CurrencyFavoritesAlertView()
                else -> CompoundInterestView()
            }
        }
    }
}

@Composable
private fun CompoundInterestView() {
    val theme = LocalDyneTheme.current
    var principalStr by remember { mutableStateOf("10000") }
    var monthlyContribStr by remember { mutableStateOf("250") }
    var annualRateStr by remember { mutableStateOf("8.0") }
    var yearsStr by remember { mutableStateOf("10") }
    var compoundFreq by remember { mutableIntStateOf(12) } // 12=Monthly, 4=Quarterly, 1=Yearly

    val p = principalStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val pmt = monthlyContribStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val r = (annualRateStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0) / 100.0
    val years = (yearsStr.toIntOrNull() ?: 1).coerceIn(1, 50)

    val yearlyBalances = remember(p, pmt, r, years, compoundFreq) {
        val list = mutableListOf<Double>()
        var bal = p
        for (y in 1..years) {
            for (m in 1..12) {
                bal += pmt
                if (compoundFreq == 12) {
                    bal *= (1.0 + r / 12.0)
                } else if (compoundFreq == 4 && m % 3 == 0) {
                    bal *= (1.0 + r / 4.0)
                } else if (compoundFreq == 1 && m == 12) {
                    bal *= (1.0 + r)
                }
            }
            list.add(bal)
        }
        list
    }

    val finalBalance = yearlyBalances.lastOrNull() ?: p
    val totalContributed = p + pmt * 12 * years
    val totalInterest = (finalBalance - totalContributed).coerceAtLeast(0.0)
    val df = remember { DecimalFormat("#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Compound Growth Parameters") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = principalStr,
                    onValueChange = { principalStr = it },
                    label = { Text("Initial Principal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = monthlyContribStr,
                    onValueChange = { monthlyContribStr = it },
                    label = { Text("Monthly Deposit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = annualRateStr,
                    onValueChange = { annualRateStr = it },
                    label = { Text("Annual Return %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = yearsStr,
                    onValueChange = { yearsStr = it },
                    label = { Text("Years") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12 to "Monthly", 4 to "Quarterly", 1 to "Yearly").forEach { (freq, label) ->
                    FilterChip(
                        selected = compoundFreq == freq,
                        onClick = { compoundFreq = freq },
                        label = { Text(label) }
                    )
                }
            }
        }

        // Growth Curve Canvas
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = theme.elevated.copy(alpha = 0.78f),
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, theme.textSecondary.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (yearlyBalances.isNotEmpty()) {
                    val maxVal = (yearlyBalances.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                    val barWidth = (size.width / yearlyBalances.size) * 0.65f
                    val gap = (size.width / yearlyBalances.size) * 0.35f
                    yearlyBalances.forEachIndexed { idx, value ->
                        val ratio = (value / maxVal).toFloat().coerceIn(0.05f, 1f)
                        val barHeight = size.height * ratio
                        val x = idx * (barWidth + gap)
                        drawRoundRect(
                            color = theme.accent.copy(alpha = 0.8f),
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                    }
                }
            }
        }

        GlassResultCard(
            headlineLabel = "Future Value (${years}Y)",
            headlineValue = df.format(finalBalance),
            rows = listOf(
                "Total Contributed" to df.format(totalContributed),
                "Compound Interest Earned" to df.format(totalInterest),
                "Yield Multiplier" to if (totalContributed > 0) "${DecimalFormat("0.00").format(finalBalance / totalContributed)}x" else "1.00x"
            )
        )
    }
}

@Composable
private fun SipCalculatorView() {
    var monthlySipStr by remember { mutableStateOf("500") }
    var expectedReturnStr by remember { mutableStateOf("12.0") }
    var yearsStr by remember { mutableStateOf("15") }

    val p = monthlySipStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val annualRate = expectedReturnStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val years = (yearsStr.toIntOrNull() ?: 1).coerceIn(1, 50)
    val n = years * 12
    val i = annualRate / 12.0 / 100.0

    val maturityValue = if (i == 0.0) {
        p * n
    } else {
        p * (((1.0 + i).pow(n) - 1.0) / i) * (1.0 + i)
    }
    val invested = p * n
    val wealthGained = (maturityValue - invested).coerceAtLeast(0.0)
    val df = remember { DecimalFormat("#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "SIP Systematic Investment Planner") {
            OutlinedTextField(
                value = monthlySipStr,
                onValueChange = { monthlySipStr = it },
                label = { Text("Monthly SIP Installment") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = expectedReturnStr,
                    onValueChange = { expectedReturnStr = it },
                    label = { Text("Expected Annual %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = yearsStr,
                    onValueChange = { yearsStr = it },
                    label = { Text("Duration (Years)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        GlassResultCard(
            headlineLabel = "SIP Maturity Value",
            headlineValue = df.format(maturityValue),
            rows = listOf(
                "Total Invested (${n} Months)" to df.format(invested),
                "Estimated Returns" to df.format(wealthGained),
                "Return Gain %" to if (invested > 0) "+${DecimalFormat("0.0").format((wealthGained / invested) * 100)}%" else "0%"
            )
        )
    }
}

@Composable
private fun LoanComparisonView() {
    var amountAStr by remember { mutableStateOf("30000") }
    var rateAStr by remember { mutableStateOf("6.9") }
    var monthsAStr by remember { mutableStateOf("48") }

    var amountBStr by remember { mutableStateOf("30000") }
    var rateBStr by remember { mutableStateOf("5.8") }
    var monthsBStr by remember { mutableStateOf("60") }

    fun computeLoan(p: Double, annualR: Double, m: Int): Pair<Double, Double> {
        val mr = annualR / 12.0 / 100.0
        val emi = if (mr == 0.0) p / m else {
            val f = (1.0 + mr).pow(m)
            p * mr * f / (f - 1.0)
        }
        val total = emi * m
        return emi to (total - p).coerceAtLeast(0.0)
    }

    val (emiA, intA) = computeLoan(
        amountAStr.toDoubleOrNull() ?: 0.0,
        rateAStr.toDoubleOrNull() ?: 0.0,
        (monthsAStr.toIntOrNull() ?: 1).coerceAtLeast(1)
    )
    val (emiB, intB) = computeLoan(
        amountBStr.toDoubleOrNull() ?: 0.0,
        rateBStr.toDoubleOrNull() ?: 0.0,
        (monthsBStr.toIntOrNull() ?: 1).coerceAtLeast(1)
    )
    val df = remember { DecimalFormat("#,##0.00") }
    val winner = if (intA <= intB) "Loan A saves ${df.format(intB - intA)} in interest" else "Loan B saves ${df.format(intA - intB)} in interest"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Loan Option A vs Option B") {
            Text("Loan Option A", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = amountAStr, onValueChange = { amountAStr = it }, label = { Text("Principal") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = rateAStr, onValueChange = { rateAStr = it }, label = { Text("Rate %") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = monthsAStr, onValueChange = { monthsAStr = it }, label = { Text("Months") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Text("Loan Option B", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = amountBStr, onValueChange = { amountBStr = it }, label = { Text("Principal") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = rateBStr, onValueChange = { rateBStr = it }, label = { Text("Rate %") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = monthsBStr, onValueChange = { monthsBStr = it }, label = { Text("Months") }, singleLine = true, modifier = Modifier.weight(1f))
            }
        }

        GlassResultCard(
            headlineLabel = "Winner",
            headlineValue = winner,
            rows = listOf(
                "Loan A Monthly EMI" to df.format(emiA),
                "Loan A Total Interest" to df.format(intA),
                "Loan B Monthly EMI" to df.format(emiB),
                "Loan B Total Interest" to df.format(intB)
            )
        )
    }
}

@Composable
private fun SavingsGoalView() {
    var targetGoalStr by remember { mutableStateOf("50000") }
    var currentSavedStr by remember { mutableStateOf("5000") }
    var annualReturnStr by remember { mutableStateOf("6.0") }
    var monthsStr by remember { mutableStateOf("36") }

    val target = targetGoalStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val current = currentSavedStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val r = (annualReturnStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0) / 12.0 / 100.0
    val months = (monthsStr.toIntOrNull() ?: 1).coerceAtLeast(1)

    val futureOfCurrent = current * (1.0 + r).pow(months)
    val shortfall = (target - futureOfCurrent).coerceAtLeast(0.0)
    val requiredMonthly = if (r == 0.0) {
        shortfall / months
    } else {
        shortfall * r / ((1.0 + r).pow(months) - 1.0)
    }
    val df = remember { DecimalFormat("#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Savings Goal Planner") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = targetGoalStr, onValueChange = { targetGoalStr = it }, label = { Text("Target Goal") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = currentSavedStr, onValueChange = { currentSavedStr = it }, label = { Text("Current Saved") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = annualReturnStr, onValueChange = { annualReturnStr = it }, label = { Text("APY %") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = monthsStr, onValueChange = { monthsStr = it }, label = { Text("Months to Goal") }, singleLine = true, modifier = Modifier.weight(1f))
            }
        }

        GlassResultCard(
            headlineLabel = "Required Monthly Saving",
            headlineValue = df.format(requiredMonthly),
            rows = listOf(
                "Target Goal" to df.format(target),
                "Current Savings Growth" to df.format(futureOfCurrent),
                "Remaining Shortfall Funded" to df.format(shortfall)
            )
        )
    }
}

private data class CurrencyPairAlert(
    val pair: String,
    val currentRate: Double,
    var targetAlertRate: Double,
    var isFavorite: Boolean = true
)

@Composable
private fun CurrencyFavoritesAlertView() {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current

    val pairs = remember {
        mutableStateListOf(
            CurrencyPairAlert("USD / EUR", 0.921, 0.910, true),
            CurrencyPairAlert("USD / GBP", 0.788, 0.780, true),
            CurrencyPairAlert("USD / JPY", 154.60, 150.00, true),
            CurrencyPairAlert("EUR / CHF", 0.954, 0.950, false),
            CurrencyPairAlert("USD / CAD", 1.385, 1.370, false),
            CurrencyPairAlert("USD / INR", 83.90, 83.25, true)
        )
    }
    var selectedPairIndex by remember { mutableIntStateOf(0) }
    var simulatedRateShift by remember { mutableFloatStateOf(0f) }

    val activePair = pairs[selectedPairIndex]
    val liveRate = activePair.currentRate * (1.0 + simulatedRateShift / 100.0)
    val alertTriggered = abs(liveRate - activePair.targetAlertRate) <= activePair.currentRate * 0.015 ||
        liveRate <= activePair.targetAlertRate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (alertTriggered) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF00E676).copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF00E676), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF00E676))
                    Column {
                        Text(
                            text = "Rate Alert Reached: ${activePair.pair}",
                            color = theme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Current rate ${DecimalFormat("0.000").format(liveRate)} crossed target ${DecimalFormat("0.000").format(activePair.targetAlertRate)}",
                            color = theme.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        GlassInputCard(title = "Favorite Currency Watchlist & Target Alert") {
            pairs.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedPairIndex == idx) theme.accent.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable {
                            FeedbackHelper.hapticTick(context)
                            selectedPairIndex = idx
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) Color(0xFFFFD54F) else theme.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    pairs[idx] = item.copy(isFavorite = !item.isFavorite)
                                }
                        )
                        Text(item.pair, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = "Rate: ${DecimalFormat("0.000").format(item.currentRate * (1.0 + simulatedRateShift / 100.0))}  •  Alert: ${DecimalFormat("0.000").format(item.targetAlertRate)}",
                        color = theme.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("Test Market Rate Shift (${DecimalFormat("+0.0;-0.0").format(simulatedRateShift)}%)", fontSize = 12.sp, color = theme.textSecondary)
            Slider(
                value = simulatedRateShift,
                onValueChange = { simulatedRateShift = it },
                valueRange = -5f..5f
            )
        }
    }
}

// ================================================================================================
// 3. SPLIT-THE-BILL PRO (Several people and items with proportional tax/tip)
// ================================================================================================

data class BillSplitItem(
    val id: Long,
    val name: String,
    val price: Double,
    val assignedPeople: Set<String>
)

@Composable
fun SplitTheBillProScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val view = LocalView.current

    val people = remember { mutableStateListOf("Alex", "Sam", "Jordan") }
    var newPersonName by remember { mutableStateOf("") }

    val items = remember {
        mutableStateListOf(
            BillSplitItem(1L, "Wood-Fired Pizza", 24.0, setOf("Alex", "Sam", "Jordan")),
            BillSplitItem(2L, "Truffle Pasta", 19.5, setOf("Alex")),
            BillSplitItem(3L, "Grilled Salmon", 26.0, setOf("Sam")),
            BillSplitItem(4L, "Pitcher & Appetizers", 21.0, setOf("Sam", "Jordan"))
        )
    }

    var newItemName by remember { mutableStateOf("") }
    var newItemPriceStr by remember { mutableStateOf("") }
    var selectedAssignees by remember { mutableStateOf(people.toSet()) }

    var taxPctStr by remember { mutableStateOf("8.5") }
    var tipPctStr by remember { mutableStateOf("18.0") }

    val subtotal = items.sumOf { it.price }
    val taxPct = taxPctStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val tipPct = tipPctStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val taxAmount = subtotal * (taxPct / 100.0)
    val tipAmount = subtotal * (tipPct / 100.0)
    val grandTotal = subtotal + taxAmount + tipAmount
    val df = remember { DecimalFormat("#,##0.00") }

    // Compute proportional breakdown per person
    val personSubtotals = remember(people.toList(), items.toList()) {
        val map = mutableMapOf<String, Double>()
        people.forEach { map[it] = 0.0 }
        items.forEach { item ->
            val active = item.assignedPeople.intersect(people.toSet())
            if (active.isNotEmpty()) {
                val share = item.price / active.size
                active.forEach { p -> map[p] = (map[p] ?: 0.0) + share }
            }
        }
        map
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassInputCard(title = "1. People at the Table (${people.size})") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(people) { person ->
                        InputChip(
                            selected = true,
                            onClick = {
                                if (people.size > 1) {
                                    people.remove(person)
                                    selectedAssignees = selectedAssignees - person
                                }
                            },
                            label = { Text(person) },
                            trailingIcon = {
                                if (people.size > 1) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove $person", modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPersonName,
                        onValueChange = { newPersonName = it },
                        label = { Text("Add Person Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val trimmed = newPersonName.trim()
                            if (trimmed.isNotEmpty() && trimmed !in people) {
                                FeedbackHelper.hapticTick(context)
                                people.add(trimmed)
                                selectedAssignees = selectedAssignees + trimmed
                                newPersonName = ""
                            }
                        },
                        modifier = Modifier.testTag("split_add_person")
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        item {
            GlassInputCard(title = "2. Add Receipt Item & Assign") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = newItemPriceStr,
                        onValueChange = { newItemPriceStr = it },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Shared by:", fontSize = 12.sp, color = theme.textSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(people) { person ->
                        val checked = person in selectedAssignees
                        FilterChip(
                            selected = checked,
                            onClick = {
                                selectedAssignees = if (checked && selectedAssignees.size > 1) {
                                    selectedAssignees - person
                                } else {
                                    selectedAssignees + person
                                }
                            },
                            label = { Text(person) }
                        )
                    }
                }
                Button(
                    onClick = {
                        val price = newItemPriceStr.toDoubleOrNull()
                        if (newItemName.isNotBlank() && price != null && price > 0) {
                            FeedbackHelper.hapticTick(context)
                            items.add(
                                BillSplitItem(
                                    id = System.currentTimeMillis(),
                                    name = newItemName.trim(),
                                    price = price,
                                    assignedPeople = selectedAssignees.ifEmpty { people.toSet() }
                                )
                            )
                            newItemName = ""
                            newItemPriceStr = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("split_add_item")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Item to Bill")
                }
            }
        }

        item {
            GlassInputCard(title = "3. Ordered Items (${items.size}) & Proportional Tax/Tip") {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = "Split: ${item.assignedPeople.joinToString(", ")}",
                                color = theme.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(df.format(item.price), color = theme.accent, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { items.remove(item) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete item", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                HorizontalDivider(color = theme.textSecondary.copy(alpha = 0.15f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taxPctStr,
                        onValueChange = { taxPctStr = it },
                        label = { Text("Tax %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tipPctStr,
                        onValueChange = { tipPctStr = it },
                        label = { Text("Tip %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            val breakdownRows = people.map { person ->
                val sub = personSubtotals[person] ?: 0.0
                val proportion = if (subtotal > 0) sub / subtotal else (1.0 / people.size.coerceAtLeast(1))
                val personTax = taxAmount * proportion
                val personTip = tipAmount * proportion
                val personTotal = sub + personTax + personTip
                "$person (Sub ${df.format(sub)} + Tax/Tip ${df.format(personTax + personTip)})" to df.format(personTotal)
            }

            GlassResultCard(
                headlineLabel = "Grand Total (Subtotal ${df.format(subtotal)})",
                headlineValue = df.format(grandTotal),
                rows = breakdownRows
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    val summary = buildString {
                        appendLine("Split-the-Bill Summary (Total: ${df.format(grandTotal)})")
                        people.forEach { person ->
                            val sub = personSubtotals[person] ?: 0.0
                            val proportion = if (subtotal > 0) sub / subtotal else 0.0
                            val personTotal = sub + (taxAmount + tipAmount) * proportion
                            appendLine("• $person owes ${df.format(personTotal)}")
                        }
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, summary)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Bill Split"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Share Per-Person Split")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ================================================================================================
// SHARED GLASS CARD HELPERS FOR TOOLS
// ================================================================================================

@Composable
fun GlassInputCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalDyneTheme.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = theme.elevated.copy(alpha = 0.8f),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, theme.textSecondary.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = theme.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            content()
        }
    }
}

@Composable
fun GlassResultCard(
    headlineLabel: String,
    headlineValue: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = theme.elevated.copy(alpha = 0.88f),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = headlineLabel,
                color = theme.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = headlineValue,
                color = theme.accent,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            if (rows.isNotEmpty()) {
                HorizontalDivider(color = theme.textSecondary.copy(alpha = 0.15f))
                rows.forEach { (k, v) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = k,
                            color = theme.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = v,
                            color = theme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
