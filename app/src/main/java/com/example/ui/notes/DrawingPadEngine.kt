package com.example.ui.notes

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.ComposeShader
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 10 Procedural Paper Surfaces generated entirely in code.
 * Free papers: Plain White, Ruled, Slate Night.
 * Premium papers: Warm Cream, Grid, Dotted, Graph, Blueprint, Kraft, Blackboard.
 */
enum class PaperStyle(
    val id: String,
    val label: String,
    val baseColorHex: String,
    val isFree: Boolean,
    val isDarkSurface: Boolean
) {
    PLAIN_WHITE("PLAIN_WHITE", "Plain White", "#FAF9F5", isFree = true, isDarkSurface = false),
    WARM_CREAM("WARM_CREAM", "Warm Cream", "#FBF2D8", isFree = false, isDarkSurface = false),
    RULED("RULED", "Ruled", "#FAF9F5", isFree = true, isDarkSurface = false),
    GRID("GRID", "Grid", "#F8FAFC", isFree = false, isDarkSurface = false),
    DOTTED("DOTTED", "Dotted", "#FAF9F6", isFree = false, isDarkSurface = false),
    GRAPH("GRAPH", "Graph", "#F3FAF6", isFree = false, isDarkSurface = false),
    BLUEPRINT("BLUEPRINT", "Blueprint", "#0B2545", isFree = false, isDarkSurface = true),
    KRAFT("KRAFT", "Kraft", "#C8A279", isFree = false, isDarkSurface = false),
    BLACKBOARD("BLACKBOARD", "Blackboard", "#17231F", isFree = false, isDarkSurface = true),
    SLATE_NIGHT("SLATE_NIGHT", "Slate Night", "#0F172A", isFree = true, isDarkSurface = true);

    val baseComposeColor: Color
        get() = Color(AndroidColor.parseColor(baseColorHex))

    companion object {
        fun fromId(id: String?): PaperStyle =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: PLAIN_WHITE
    }
}

/**
 * 6 realistic pens in the Full-Screen Drawing Pad.
 * Free pens (3): Ballpoint, Pencil, Highlighter.
 * Premium pens (3): Fountain (pressure sensitive), Marker, Chalk.
 */
enum class SketchPenType(
    val id: String,
    val label: String,
    val subtitle: String,
    val isFree: Boolean,
    val defaultWidthDp: Float
) {
    BALLPOINT("BALLPOINT", "Ballpoint", "Crisp smooth ink", isFree = true, defaultWidthDp = 4.5f),
    FOUNTAIN("FOUNTAIN", "Fountain", "Pressure & tilt nib", isFree = false, defaultWidthDp = 6.5f),
    MARKER("MARKER", "Marker", "Bold felt chisel", isFree = false, defaultWidthDp = 10f),
    PENCIL("PENCIL", "Pencil", "Grainy graphite", isFree = true, defaultWidthDp = 4f),
    CHALK("CHALK", "Chalk", "Textured pastel", isFree = false, defaultWidthDp = 11f),
    HIGHLIGHTER("HIGHLIGHTER", "Highlighter", "Translucent pass", isFree = true, defaultWidthDp = 18f);

    companion object {
        fun fromId(id: String?): SketchPenType =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: BALLPOINT
    }
}

/**
 * Floating glass tool dock modes in the Full-Screen Drawing Pad.
 */
enum class DrawingDockMode(val label: String) {
    PEN("Pen"),
    HIGHLIGHTER("Highlighter"),
    ERASER("Eraser"),
    LASSO("Lasso")
}

data class StylusPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.65f,
    val tilt: Float = 0f,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun toOffset(): Offset = Offset(x, y)
}

data class SketchStroke(
    val id: Long = System.nanoTime(),
    val points: List<StylusPoint>,
    val colorHex: String,
    val penType: SketchPenType,
    val isEraser: Boolean = false,
    val strokeWidthPx: Float = 8f,
    val snappedShapeName: String? = null
)

/**
 * 12 curated ink colors + custom picker support.
 */
val DrawingInkColors12: List<NoteColorOption> = listOf(
    NoteColorOption("#111827", "Obsidian Ink", Color(0xFF111827)),
    NoteColorOption("#FFFFFF", "Chalk White", Color(0xFFFFFFFF)),
    NoteColorOption("#1D4ED8", "Royal Blue", Color(0xFF1D4ED8)),
    NoteColorOption("#38BDF8", "Sky Cyan", Color(0xFF38BDF8)),
    NoteColorOption("#EF4444", "Crimson Red", Color(0xFFEF4444)),
    NoteColorOption("#FB7185", "Coral Rose", Color(0xFFFB7185)),
    NoteColorOption("#10B981", "Emerald", Color(0xFF10B981)),
    NoteColorOption("#C6F27A", "Electric Lime", Color(0xFFC6F27A)),
    NoteColorOption("#F59E0B", "Amber Gold", Color(0xFFF59E0B)),
    NoteColorOption("#8B5CF6", "Vivid Violet", Color(0xFF8B5CF6)),
    NoteColorOption("#EC4899", "Magenta", Color(0xFFEC4899)),
    NoteColorOption("#78350F", "Sepia Umber", Color(0xFF78350F))
)

/**
 * Checklist item model for Checklists segment.
 */
data class ChecklistItemData(
    val id: Long = System.nanoTime(),
    val text: String,
    val checked: Boolean = false
)

object ChecklistSerializer {
    fun toJson(items: List<ChecklistItemData>): String {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("checked", item.checked)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun fromJson(json: String, fallbackContent: String = ""): List<ChecklistItemData> {
        if (json.isNotBlank()) {
            runCatching {
                val arr = JSONArray(json)
                val list = ArrayList<ChecklistItemData>(arr.length())
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ChecklistItemData(
                            id = obj.optLong("id", System.nanoTime() + i),
                            text = obj.optString("text", ""),
                            checked = obj.optBoolean("checked", false)
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            }
        }
        if (fallbackContent.isNotBlank()) {
            return fallbackContent.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapIndexed { idx, line ->
                    val checked = line.startsWith("[x]", ignoreCase = true) || line.startsWith("✓")
                    val clean = line
                        .removePrefix("[x]")
                        .removePrefix("[X]")
                        .removePrefix("[ ]")
                        .removePrefix("✓")
                        .removePrefix("•")
                        .removePrefix("-")
                        .trim()
                    ChecklistItemData(
                        id = System.nanoTime() + idx,
                        text = clean.ifBlank { line },
                        checked = checked
                    )
                }
        }
        return emptyList()
    }

    fun toPlainSummary(items: List<ChecklistItemData>): String {
        return items.joinToString("\n") { item ->
            if (item.checked) "[x] ${item.text}" else "[ ] ${item.text}"
        }
    }
}

/**
 * 4 Free Note & Checklist Templates:
 * Shopping list, Meeting notes, Trip checklist, Daily plan.
 */
data class FreeNoteTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val noteType: String, // "TEXT" or "CHECKLIST"
    val colorHex: String,
    val tagsCsv: String,
    val folderName: String,
    val bodyContent: String,
    val checklistItems: List<ChecklistItemData> = emptyList()
)

val FreeNoteTemplates: List<FreeNoteTemplate> = listOf(
    FreeNoteTemplate(
        id = "shopping_list",
        title = "Shopping list",
        subtitle = "Groceries & essentials",
        noteType = "CHECKLIST",
        colorHex = "#C6F27A",
        tagsCsv = "shopping,groceries",
        folderName = "Personal",
        bodyContent = "",
        checklistItems = listOf(
            ChecklistItemData(id = 101L, text = "Fresh produce & greens", checked = false),
            ChecklistItemData(id = 102L, text = "Milk / oat milk & eggs", checked = false),
            ChecklistItemData(id = 103L, text = "Whole grain bread & coffee beans", checked = false),
            ChecklistItemData(id = 104L, text = "Olive oil & pantry staples", checked = false),
            ChecklistItemData(id = 105L, text = "Household batteries / supplies", checked = false)
        )
    ),
    FreeNoteTemplate(
        id = "meeting_notes",
        title = "Meeting notes",
        subtitle = "Agenda, decisions & next steps",
        noteType = "TEXT",
        colorHex = "#38BDF8",
        tagsCsv = "meeting,work",
        folderName = "Work",
        bodyContent = buildString {
            appendLine("Attendees: ")
            appendLine("Goal: ")
            appendLine()
            appendLine("Key Discussion Points:")
            appendLine("• ")
            appendLine("• ")
            appendLine()
            appendLine("Action Items & Owners:")
            appendLine("1. ")
            appendLine("2. ")
        }.trimEnd()
    ),
    FreeNoteTemplate(
        id = "trip_checklist",
        title = "Trip checklist",
        subtitle = "Packing & travel prep",
        noteType = "CHECKLIST",
        colorHex = "#34D399",
        tagsCsv = "travel,packing",
        folderName = "Travel",
        bodyContent = "",
        checklistItems = listOf(
            ChecklistItemData(id = 201L, text = "Passport / ID & boarding passes", checked = false),
            ChecklistItemData(id = 202L, text = "Phone charger, power bank & cables", checked = false),
            ChecklistItemData(id = 203L, text = "Weather-ready jacket & footwear", checked = false),
            ChecklistItemData(id = 204L, text = "Medications & first-aid kit", checked = false),
            ChecklistItemData(id = 205L, text = "Offline maps & hotel confirmation", checked = false)
        )
    ),
    FreeNoteTemplate(
        id = "daily_plan",
        title = "Daily plan",
        subtitle = "Top 3 priorities & schedule",
        noteType = "TEXT",
        colorHex = "#A78BFA",
        tagsCsv = "daily,focus",
        folderName = "Daily",
        bodyContent = buildString {
            appendLine("Top 3 Priorities Today:")
            appendLine("1. ")
            appendLine("2. ")
            appendLine("3. ")
            appendLine()
            appendLine("Schedule / Time Blocks:")
            appendLine("• Morning: ")
            appendLine("• Afternoon: ")
            appendLine("• Evening: ")
            appendLine()
            appendLine("End-of-Day Reflection:")
            appendLine("• ")
        }.trimEnd()
    )
)

/**
 * Procedural Paper & 60fps Stroke Rendering Engine.
 */
object DrawingPadEngine {

    private var cachedFineGrainShader: BitmapShader? = null
    private var cachedCoarseGrainShader: BitmapShader? = null

    /**
     * Generates a tileable 64x64 procedural noise shader used for realistic paper grain,
     * graphite pencil tooth, and dusty chalk stippling.
     */
    fun getFinePaperGrainShader(): BitmapShader {
        cachedFineGrainShader?.let { return it }
        val size = 64
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(421337L)
        val pixels = IntArray(size * size)
        for (i in pixels.indices) {
            val v = rng.nextInt(0, 255)
            val alpha = rng.nextInt(5, 18)
            val shade = if (v > 128) 255 else 0
            pixels[i] = AndroidColor.argb(alpha, shade, shade, shade)
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).also {
            cachedFineGrainShader = it
        }
    }

    /**
     * Generates a coarser stipple texture shader for Pencil (graphite) and Chalk pens.
     */
    fun getCoarseBrushGrainShader(): BitmapShader {
        cachedCoarseGrainShader?.let { return it }
        val size = 48
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(987654L)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val n = rng.nextFloat()
                val alpha = when {
                    n < 0.22f -> 40
                    n < 0.55f -> 155
                    else -> 240
                }
                pixels[y * size + x] = AndroidColor.argb(alpha, 255, 255, 255)
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).also {
            cachedCoarseGrainShader = it
        }
    }

    /**
     * Renders a complete procedural paper surface onto [canvas] of [width] x [height]:
     * 1. Base paper tone
     * 2. Procedural noise grain tile via BitmapShader
     * 3. Subtle organic paper fibers
     * 4. Surface rules/grids/dots/blueprint/kraft/blackboard markings
     * 5. Soft radial vignette + physical sheet inner edge highlight
     */
    fun drawProceduralPaper(
        canvas: AndroidCanvas,
        paper: PaperStyle,
        width: Int,
        height: Int
    ) {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val baseColor = AndroidColor.parseColor(paper.baseColorHex)
        canvas.drawColor(baseColor)

        // 1. Subtle procedural noise grain tile across the entire page
        val grainPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            shader = getFinePaperGrainShader()
            alpha = when (paper) {
                PaperStyle.KRAFT -> 235
                PaperStyle.BLACKBOARD -> 220
                PaperStyle.WARM_CREAM -> 170
                else -> 130
            }
        }
        canvas.drawRect(0f, 0f, w, h, grainPaint)

        // 2. Faint procedural organic fibers
        val fiberRng = Random(paper.ordinal * 7919L + 101L)
        val fiberCount = when (paper) {
            PaperStyle.KRAFT -> 95
            PaperStyle.WARM_CREAM, PaperStyle.PLAIN_WHITE, PaperStyle.RULED -> 42
            PaperStyle.BLACKBOARD -> 55
            else -> 28
        }
        val fiberPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.STROKE
            strokeWidth = 1.1f
            color = if (paper.isDarkSurface) {
                AndroidColor.argb(14, 255, 255, 255)
            } else if (paper == PaperStyle.KRAFT) {
                AndroidColor.argb(28, 88, 52, 22)
            } else {
                AndroidColor.argb(11, 110, 95, 75)
            }
        }
        val fiberPath = AndroidPath()
        for (i in 0 until fiberCount) {
            val sx = fiberRng.nextFloat() * w
            val sy = fiberRng.nextFloat() * h
            val len = 10f + fiberRng.nextFloat() * 26f
            val angle = fiberRng.nextFloat() * (PI.toFloat() * 2f)
            val cx = sx + cos(angle + 0.35f) * (len * 0.5f)
            val cy = sy + sin(angle + 0.35f) * (len * 0.5f)
            val ex = sx + cos(angle) * len
            val ey = sy + sin(angle) * len
            fiberPath.reset()
            fiberPath.moveTo(sx, sy)
            fiberPath.quadTo(cx, cy, ex, ey)
            canvas.drawPath(fiberPath, fiberPaint)
        }

        // 3. Paper-specific ruling / grid / pattern
        val step = (w / 18f).coerceIn(22f, 48f)
        when (paper) {
            PaperStyle.PLAIN_WHITE, PaperStyle.WARM_CREAM -> {
                // Clean stationery with grain, fibers, vignette
            }

            PaperStyle.RULED -> {
                val linePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(72, 96, 165, 250) // classic notebook sky-blue
                    strokeWidth = 1.35f
                }
                val headerY = step * 2.1f
                var y = headerY
                while (y < h - step * 0.6f) {
                    canvas.drawLine(0f, y, w, y, linePaint)
                    y += step * 1.15f
                }
                // Double red left margin rule
                val marginPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(95, 244, 63, 94)
                    strokeWidth = 1.5f
                }
                val marginX = w * 0.12f
                canvas.drawLine(marginX, 0f, marginX, h, marginPaint)
                canvas.drawLine(marginX + 4f, 0f, marginX + 4f, h, marginPaint)
            }

            PaperStyle.GRID -> {
                val gridPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(46, 71, 85, 105)
                    strokeWidth = 1.1f
                }
                var x = step
                while (x < w) {
                    canvas.drawLine(x, 0f, x, h, gridPaint)
                    x += step
                }
                var y = step
                while (y < h) {
                    canvas.drawLine(0f, y, w, y, gridPaint)
                    y += step
                }
            }

            PaperStyle.DOTTED -> {
                val dotPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(85, 100, 116, 139)
                    style = AndroidPaint.Style.FILL
                }
                var y = step
                while (y < h - step * 0.5f) {
                    var x = step
                    while (x < w - step * 0.5f) {
                        canvas.drawCircle(x, y, 1.85f, dotPaint)
                        x += step
                    }
                    y += step
                }
            }

            PaperStyle.GRAPH -> {
                val minorStep = step / 4f
                val minorPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(26, 16, 185, 129)
                    strokeWidth = 0.8f
                }
                val majorPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(72, 5, 150, 105)
                    strokeWidth = 1.4f
                }
                var col = 1
                var x = minorStep
                while (x < w) {
                    canvas.drawLine(x, 0f, x, h, if (col % 4 == 0) majorPaint else minorPaint)
                    x += minorStep
                    col++
                }
                var row = 1
                var y = minorStep
                while (y < h) {
                    canvas.drawLine(0f, y, w, y, if (row % 4 == 0) majorPaint else minorPaint)
                    y += minorStep
                    row++
                }
            }

            PaperStyle.BLUEPRINT -> {
                val minorStep = step / 4f
                val minorPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(28, 56, 189, 248)
                    strokeWidth = 0.8f
                }
                val majorPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(78, 125, 211, 252)
                    strokeWidth = 1.35f
                }
                var col = 1
                var x = minorStep
                while (x < w) {
                    canvas.drawLine(x, 0f, x, h, if (col % 4 == 0) majorPaint else minorPaint)
                    x += minorStep
                    col++
                }
                var row = 1
                var y = minorStep
                while (y < h) {
                    canvas.drawLine(0f, y, w, y, if (row % 4 == 0) majorPaint else minorPaint)
                    y += minorStep
                    row++
                }
                // Technical architectural border + title block corner
                val frameInset = 18f
                val framePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(145, 186, 230, 253)
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = 1.8f
                }
                canvas.drawRect(frameInset, frameInset, w - frameInset, h - frameInset, framePaint)
            }

            PaperStyle.KRAFT -> {
                // Subtle horizontal laid-paper rib lines
                val ribPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(18, 68, 40, 18)
                    strokeWidth = 1f
                }
                var y = 6f
                while (y < h) {
                    canvas.drawLine(0f, y, w, y, ribPaint)
                    y += 7f
                }
            }

            PaperStyle.BLACKBOARD -> {
                // Soft chalk dust haze near the center of the board
                val chalkDustPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.5f,
                        h * 0.45f,
                        max(w, h) * 0.55f,
                        intArrayOf(
                            AndroidColor.argb(18, 255, 255, 255),
                            AndroidColor.argb(6, 255, 255, 255),
                            AndroidColor.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, chalkDustPaint)
            }

            PaperStyle.SLATE_NIGHT -> {
                val subtleDotPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(38, 148, 163, 184)
                    style = AndroidPaint.Style.FILL
                }
                var y = step
                while (y < h - step * 0.5f) {
                    var x = step
                    while (x < w - step * 0.5f) {
                        canvas.drawCircle(x, y, 1.35f, subtleDotPaint)
                        x += step
                    }
                    y += step
                }
            }
        }

        // 4. Soft radial vignette around the page perimeter
        val vignetteRadius = hypot(w * 0.5f, h * 0.5f).coerceAtLeast(10f)
        val vignetteEdgeAlpha = if (paper.isDarkSurface) 68 else 28
        val vignettePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f,
                h * 0.5f,
                vignetteRadius,
                intArrayOf(
                    AndroidColor.TRANSPARENT,
                    AndroidColor.TRANSPARENT,
                    AndroidColor.argb(vignetteEdgeAlpha, 0, 0, 0)
                ),
                floatArrayOf(0f, 0.68f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, vignettePaint)

        // 5. Crisp inner paper edge bevel
        val borderPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.STROKE
            strokeWidth = 2f
            color = if (paper.isDarkSurface) {
                AndroidColor.argb(45, 255, 255, 255)
            } else {
                AndroidColor.argb(35, 15, 23, 42)
            }
        }
        canvas.drawRect(1f, 1f, w - 1f, h - 1f, borderPaint)
    }

    /**
     * Builds a smooth quadratic midpoint Bezier path from [points].
     */
    fun buildSmoothBezierPath(
        points: List<StylusPoint>,
        scaleX: Float = 1f,
        scaleY: Float = 1f
    ): AndroidPath {
        val path = AndroidPath()
        if (points.isEmpty()) return path
        val p0x = points[0].x * scaleX
        val p0y = points[0].y * scaleY
        path.moveTo(p0x, p0y)
        if (points.size == 1) {
            path.lineTo(p0x + 0.1f, p0y + 0.1f)
            return path
        }
        for (i in 1 until points.size - 1) {
            val currX = points[i].x * scaleX
            val currY = points[i].y * scaleY
            val nextX = points[i + 1].x * scaleX
            val nextY = points[i + 1].y * scaleY
            val midX = (currX + nextX) * 0.5f
            val midY = (currY + nextY) * 0.5f
            path.quadTo(currX, currY, midX, midY)
        }
        val last = points.last()
        path.lineTo(last.x * scaleX, last.y * scaleY)
        return path
    }

    /**
     * Draws a single [SketchStroke] onto an [AndroidCanvas] with realistic pen physics:
     * - Ballpoint: crisp smooth Bezier curve
     * - Fountain: variable-width pressure & tilt sensitive calligraphy segments
     * - Marker: bold chisel-tip stroke with rich ink
     * - Pencil: grainy graphite shader modulated by pressure
     * - Chalk: wide textured pastel stroke with stipple grain
     * - Highlighter: translucent wide chisel pass
     */
    fun drawStrokeOnAndroidCanvas(
        canvas: AndroidCanvas,
        stroke: SketchStroke,
        paper: PaperStyle,
        scaleX: Float = 1f,
        scaleY: Float = 1f
    ) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        if (stroke.isEraser) {
            // Erase back to paper base color
            val erasePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor(paper.baseColorHex)
                style = AndroidPaint.Style.STROKE
                strokeWidth = stroke.strokeWidthPx * 2.4f * scaleX
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
            }
            val path = buildSmoothBezierPath(pts, scaleX, scaleY)
            canvas.drawPath(path, erasePaint)
            return
        }

        val rawColor = runCatching { AndroidColor.parseColor(stroke.colorHex) }
            .getOrDefault(AndroidColor.BLACK)
        val baseWidth = (stroke.strokeWidthPx * scaleX).coerceAtLeast(1.5f)

        when (stroke.penType) {
            SketchPenType.BALLPOINT -> {
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = rawColor
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = baseWidth
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                }
                canvas.drawPath(buildSmoothBezierPath(pts, scaleX, scaleY), paint)
            }

            SketchPenType.FOUNTAIN -> {
                // Pressure & tilt sensitive variable-width calligraphy segments
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = rawColor
                    style = AndroidPaint.Style.STROKE
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                }
                if (pts.size <= 2) {
                    paint.strokeWidth = baseWidth
                    canvas.drawPath(buildSmoothBezierPath(pts, scaleX, scaleY), paint)
                } else {
                    for (i in 0 until pts.size - 1) {
                        val a = pts[i]
                        val b = pts[i + 1]
                        val dist = hypot((b.x - a.x) * scaleX, (b.y - a.y) * scaleY)
                        val velocityFactor = (1f - (dist / 55f).coerceIn(0f, 0.55f))
                        val pressureFactor = ((a.pressure + b.pressure) * 0.5f).coerceIn(0.25f, 1.45f)
                        val tiltFactor = 1f + ((a.tilt + b.tilt) * 0.25f).coerceIn(0f, 0.4f)
                        paint.strokeWidth = (baseWidth * pressureFactor * velocityFactor * tiltFactor)
                            .coerceIn(baseWidth * 0.35f, baseWidth * 1.85f)
                        canvas.drawLine(
                            a.x * scaleX,
                            a.y * scaleY,
                            b.x * scaleX,
                            b.y * scaleY,
                            paint
                        )
                    }
                }
            }

            SketchPenType.MARKER -> {
                val coreColor = AndroidColor.argb(
                    225,
                    AndroidColor.red(rawColor),
                    AndroidColor.green(rawColor),
                    AndroidColor.blue(rawColor)
                )
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = coreColor
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = baseWidth * 1.45f
                    strokeCap = AndroidPaint.Cap.SQUARE
                    strokeJoin = AndroidPaint.Join.BEVEL
                }
                canvas.drawPath(buildSmoothBezierPath(pts, scaleX, scaleY), paint)
            }

            SketchPenType.PENCIL -> {
                val avgPressure = pts.map { it.pressure }.average().toFloat().coerceIn(0.3f, 1.2f)
                val alpha = (165 * avgPressure).roundToInt().coerceIn(85, 220)
                val graphiteColor = AndroidColor.argb(
                    alpha,
                    AndroidColor.red(rawColor),
                    AndroidColor.green(rawColor),
                    AndroidColor.blue(rawColor)
                )
                val path = buildSmoothBezierPath(pts, scaleX, scaleY)
                val basePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = graphiteColor
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = baseWidth * 0.95f
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                    // Subtle micro-dash to mimic paper tooth on graphite
                    pathEffect = DashPathEffect(floatArrayOf(baseWidth * 1.4f, baseWidth * 0.25f), 0f)
                }
                canvas.drawPath(path, basePaint)

                // Second thin core pass for realistic graphite density
                val corePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(
                        (alpha * 0.7f).roundToInt(),
                        AndroidColor.red(rawColor),
                        AndroidColor.green(rawColor),
                        AndroidColor.blue(rawColor)
                    )
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = baseWidth * 0.55f
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                }
                canvas.drawPath(path, corePaint)
            }

            SketchPenType.CHALK -> {
                val path = buildSmoothBezierPath(pts, scaleX, scaleY)
                // Dusty outer halo pass
                val outerPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(
                        95,
                        AndroidColor.red(rawColor),
                        AndroidColor.green(rawColor),
                        AndroidColor.blue(rawColor)
                    )
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = baseWidth * 1.65f
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                    pathEffect = DashPathEffect(floatArrayOf(baseWidth * 0.8f, baseWidth * 0.45f), 2f)
                }
                canvas.drawPath(path, outerPaint)

                // Textured inner chalk pass
                val innerPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb(
                        190,
                        AndroidColor.red(rawColor),
                        AndroidColor.green(rawColor),
                        AndroidColor.blue(rawColor)
                    )
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = baseWidth * 1.15f
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                    pathEffect = DashPathEffect(floatArrayOf(baseWidth * 1.25f, baseWidth * 0.35f), 0f)
                }
                canvas.drawPath(path, innerPaint)
            }

            SketchPenType.HIGHLIGHTER -> {
                val hlColor = AndroidColor.argb(
                    88,
                    AndroidColor.red(rawColor),
                    AndroidColor.green(rawColor),
                    AndroidColor.blue(rawColor)
                )
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = hlColor
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = baseWidth * 2.2f
                    strokeCap = AndroidPaint.Cap.SQUARE
                    strokeJoin = AndroidPaint.Join.BEVEL
                }
                canvas.drawPath(buildSmoothBezierPath(pts, scaleX, scaleY), paint)
            }
        }
    }

    /**
     * Renders the full sheet (procedural paper + all committed strokes) into a high-resolution Bitmap
     * for 60fps display, PNG saving, and PDF export.
     */
    fun renderFullSheetToBitmap(
        paper: PaperStyle,
        strokes: List<SketchStroke>,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int = 1080
    ): Bitmap {
        val safeSrcW = sourceWidth.coerceAtLeast(320)
        val safeSrcH = sourceHeight.coerceAtLeast(450)
        val aspect = safeSrcH.toFloat() / safeSrcW.toFloat()
        val outW = targetWidth.coerceIn(600, 1600)
        val outH = (outW * aspect).roundToInt().coerceIn(800, 2600)

        val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        drawProceduralPaper(canvas, paper, outW, outH)

        val scaleX = outW.toFloat() / safeSrcW.toFloat()
        val scaleY = outH.toFloat() / safeSrcH.toFloat()
        for (stroke in strokes) {
            drawStrokeOnAndroidCanvas(
                canvas = canvas,
                stroke = stroke,
                paper = paper,
                scaleX = scaleX,
                scaleY = scaleY
            )
        }
        return bmp
    }

    /**
     * Premium Shape Recognition:
     * When the user holds still at the end of a stroke, snaps rough hand-drawn strokes into
     * crisp geometric Lines, Circles/Ellipses, or Rectangles.
     */
    fun trySnapRecognizedShape(points: List<StylusPoint>): Pair<String, List<StylusPoint>>? {
        if (points.size < 6) return null
        val first = points.first()
        val last = points.last()

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var pathLen = 0f

        for (i in points.indices) {
            val p = points[i]
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
            if (i > 0) {
                pathLen += hypot(p.x - points[i - 1].x, p.y - points[i - 1].y)
            }
        }

        val w = maxX - minX
        val h = maxY - minY
        val diag = hypot(w, h)
        if (diag < 36f || pathLen < 36f) return null

        val chordLen = hypot(last.x - first.x, last.y - first.y)

        // 1. Check for Straight Line: chord length is close to path length and max perpendicular deviation is small
        if (chordLen > pathLen * 0.86f) {
            val dx = last.x - first.x
            val dy = last.y - first.y
            var maxPerp = 0f
            for (p in points) {
                val perp = abs(dy * p.x - dx * p.y + last.x * first.y - last.y * first.x) / chordLen.coerceAtLeast(1f)
                if (perp > maxPerp) maxPerp = perp
            }
            if (maxPerp < diag * 0.085f) {
                val snapped = (0..16).map { step ->
                    val t = step / 16f
                    StylusPoint(
                        x = first.x + dx * t,
                        y = first.y + dy * t,
                        pressure = 0.75f,
                        timestampMs = first.timestampMs + step * 10L
                    )
                }
                return "Line" to snapped
            }
        }

        // 2. Closed shapes (Circle or Rectangle): start and end are reasonably close
        if (chordLen < diag * 0.42f && w > 28f && h > 28f) {
            val cx = (minX + maxX) * 0.5f
            val cy = (minY + maxY) * 0.5f
            val rx = (w * 0.5f).coerceAtLeast(1f)
            val ry = (h * 0.5f).coerceAtLeast(1f)

            // Normalized radial distance from ellipse: (x - cx)^2 / rx^2 + (y - cy)^2 / ry^2 == 1
            var radialErrorSum = 0f
            var edgeBoxErrorSum = 0f
            for (p in points) {
                val nx = (p.x - cx) / rx
                val ny = (p.y - cy) / ry
                val r = sqrt(nx * nx + ny * ny)
                radialErrorSum += abs(r - 1f)

                val distToNearestBoxEdge = minOf(
                    abs(p.x - minX) / w,
                    abs(p.x - maxX) / w,
                    abs(p.y - minY) / h,
                    abs(p.y - maxY) / h
                )
                edgeBoxErrorSum += distToNearestBoxEdge
            }
            val avgRadialError = radialErrorSum / points.size
            val avgBoxError = edgeBoxErrorSum / points.size

            return if (avgRadialError <= avgBoxError * 1.35f && avgRadialError < 0.26f) {
                // Snap to Circle (if aspect ratio close to 1:1) or Ellipse
                val isCircle = abs(w - h) / max(w, h) < 0.25f
                val finalRx = if (isCircle) (rx + ry) * 0.5f else rx
                val finalRy = if (isCircle) (rx + ry) * 0.5f else ry
                val steps = 48
                val snapped = (0..steps).map { idx ->
                    val theta = (idx.toFloat() / steps.toFloat()) * (2f * PI.toFloat())
                    StylusPoint(
                        x = cx + cos(theta) * finalRx,
                        y = cy + sin(theta) * finalRy,
                        pressure = 0.75f,
                        timestampMs = first.timestampMs + idx * 8L
                    )
                }
                (if (isCircle) "Circle" else "Ellipse") to snapped
            } else if (avgBoxError < 0.18f) {
                // Snap to Rectangle
                val corners = listOf(
                    Offset(minX, minY),
                    Offset(maxX, minY),
                    Offset(maxX, maxY),
                    Offset(minX, maxY),
                    Offset(minX, minY)
                )
                val snapped = ArrayList<StylusPoint>()
                var tStep = 0L
                for (edge in 0 until 4) {
                    val start = corners[edge]
                    val end = corners[edge + 1]
                    for (sub in 0..8) {
                        val f = sub / 8f
                        snapped.add(
                            StylusPoint(
                                x = start.x + (end.x - start.x) * f,
                                y = start.y + (end.y - start.y) * f,
                                pressure = 0.75f,
                                timestampMs = first.timestampMs + (tStep++ * 8L)
                            )
                        )
                    }
                }
                "Rectangle" to snapped
            } else {
                null
            }
        }

        return null
    }

    /**
     * Serializes strokes to compact JSON so saved sketches remain editable in the Drawing Pad.
     */
    fun serializeStrokes(strokes: List<SketchStroke>): String {
        val arr = JSONArray()
        for (s in strokes) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("colorHex", s.colorHex)
            obj.put("penType", s.penType.id)
            obj.put("isEraser", s.isEraser)
            obj.put("strokeWidthPx", s.strokeWidthPx.toDouble())
            if (s.snappedShapeName != null) obj.put("snappedShapeName", s.snappedShapeName)
            val ptsArr = JSONArray()
            // Cap serialized points per stroke to 180 to keep Room rows fast and compact
            val stride = ((s.points.size / 180) + 1).coerceAtLeast(1)
            for (i in s.points.indices step stride) {
                val p = s.points[i]
                val pObj = JSONObject()
                pObj.put("x", (p.x * 10f).roundToInt() / 10.0)
                pObj.put("y", (p.y * 10f).roundToInt() / 10.0)
                pObj.put("p", (p.pressure * 100f).roundToInt() / 100.0)
                pObj.put("t", p.timestampMs)
                ptsArr.put(pObj)
            }
            obj.put("points", ptsArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeStrokes(json: String): List<SketchStroke> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            val list = ArrayList<SketchStroke>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ptsArr = obj.optJSONArray("points") ?: continue
                val pts = ArrayList<StylusPoint>(ptsArr.length())
                for (j in 0 until ptsArr.length()) {
                    val pObj = ptsArr.getJSONObject(j)
                    pts.add(
                        StylusPoint(
                            x = pObj.optDouble("x", 0.0).toFloat(),
                            y = pObj.optDouble("y", 0.0).toFloat(),
                            pressure = pObj.optDouble("p", 0.65).toFloat(),
                            timestampMs = pObj.optLong("t", System.currentTimeMillis())
                        )
                    )
                }
                if (pts.isNotEmpty()) {
                    list.add(
                        SketchStroke(
                            id = obj.optLong("id", System.nanoTime() + i),
                            points = pts,
                            colorHex = obj.optString("colorHex", "#111827"),
                            penType = SketchPenType.fromId(obj.optString("penType", "BALLPOINT")),
                            isEraser = obj.optBoolean("isEraser", false),
                            strokeWidthPx = obj.optDouble("strokeWidthPx", 8.0).toFloat(),
                            snappedShapeName = obj.optString("snappedShapeName", "").ifBlank { null }
                        )
                    )
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    /**
     * Premium Handwriting-to-Text via ML Kit Digital Ink Recognition (`com.google.mlkit:digital-ink-recognition`).
     * Downloads the recognition model on first use and skips gracefully if offline/unavailable.
     */
    fun recognizeHandwritingWithMlKit(
        strokes: List<SketchStroke>,
        onStatus: (String) -> Unit,
        onResult: (recognizedText: String?, message: String) -> Unit
    ) {
        val inkStrokes = strokes.filter { !it.isEraser && it.points.isNotEmpty() }
        if (inkStrokes.isEmpty()) {
            onResult(null, "Draw handwritten words on the canvas first.")
            return
        }

        runCatching {
            val inkBuilder = Ink.builder()
            var syntheticTime = 1_000L
            for (stroke in inkStrokes) {
                val strokeBuilder = Ink.Stroke.builder()
                for (pt in stroke.points) {
                    val t = if (pt.timestampMs > 0L) pt.timestampMs else syntheticTime
                    syntheticTime += 16L
                    strokeBuilder.addPoint(Ink.Point.create(pt.x, pt.y, t))
                }
                inkBuilder.addStroke(strokeBuilder.build())
            }
            val ink = inkBuilder.build()

            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            if (modelIdentifier == null) {
                onResult(null, "Digital Ink model unavailable on this device.")
                return
            }
            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            val remoteModelManager = RemoteModelManager.getInstance()

            fun runRecognizer() {
                val recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(model).build()
                )
                recognizer.recognize(ink)
                    .addOnSuccessListener { result ->
                        val text = result.candidates.firstOrNull()?.text?.trim().orEmpty()
                        recognizer.close()
                        if (text.isNotEmpty()) {
                            onResult(text, "Handwriting recognized: \"$text\"")
                        } else {
                            onResult(null, "No handwriting text detected in strokes.")
                        }
                    }
                    .addOnFailureListener {
                        recognizer.close()
                        onResult(null, "Handwriting recognition unavailable right now.")
                    }
            }

            remoteModelManager.isModelDownloaded(model)
                .addOnSuccessListener { isDownloaded ->
                    if (isDownloaded) {
                        onStatus("Recognizing handwriting...")
                        runRecognizer()
                    } else {
                        onStatus("Downloading handwriting model (first use)...")
                        remoteModelManager.download(model, DownloadConditions.Builder().build())
                            .addOnSuccessListener {
                                onStatus("Recognizing handwriting...")
                                runRecognizer()
                            }
                            .addOnFailureListener {
                                onResult(
                                    null,
                                    "Handwriting model download skipped (offline). Connect once to download."
                                )
                            }
                    }
                }
                .addOnFailureListener {
                    onResult(null, "Handwriting recognition skipped (Play Services unavailable).")
                }
        }.onFailure {
            onResult(null, "Handwriting recognition skipped gracefully.")
        }
    }
}
