package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class PdfCompressionLevel(
    val label: String,
    val subtitle: String,
    val targetDpi: Int,
    val jpegQuality: Int
) {
    LOW("Low", "High quality • ~180 DPI (82%)", 180, 82),
    MEDIUM("Medium", "Balanced • ~130 DPI (68%)", 130, 68),
    HIGH("High", "Smallest file • ~92 DPI (50%)", 92, 50);

    val id: String
        get() = name.lowercase(Locale.US)
}

data class PdfCompressResult(
    val beforeBytes: Long,
    val afterBytes: Long,
    val outputFile: File
) {
    val savedBytes: Long
        get() = (beforeBytes - afterBytes).coerceAtLeast(0L)

    val savedPercent: Int
        get() = if (beforeBytes > 0L && afterBytes < beforeBytes) {
            (((beforeBytes - afterBytes).toDouble() / beforeBytes.toDouble()) * 100.0).roundToInt().coerceIn(1, 95)
        } else {
            0
        }
}

enum class WatermarkPosition(val label: String) {
    CENTER_DIAGONAL("Diagonal Center"),
    CENTER("Center"),
    TOP("Top"),
    BOTTOM("Bottom")
}

typealias WatermarkConfig = PdfWatermarkConfig

data class PdfWatermarkConfig(
    val enabled: Boolean = false,
    val text: String = "CONFIDENTIAL",
    val imagePath: String = "",
    val opacity: Float = 0.25f,
    val position: WatermarkPosition = WatermarkPosition.CENTER_DIAGONAL
)

enum class PageNumberFormat(val label: String) {
    PAGE_X_OF_N("Page 1 of N"),
    X_SLASH_N("1 / N"),
    NUMBER_ONLY("1, 2, 3")
}

typealias PageNumberPosition = PageOverlayPosition

enum class PageOverlayPosition(val label: String) {
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right"),
    TOP_RIGHT("Top Right")
}

typealias PageNumberConfig = PdfPageNumberConfig

data class PdfPageNumberConfig(
    val enabled: Boolean = false,
    val format: PageNumberFormat = PageNumberFormat.PAGE_X_OF_N,
    val position: PageOverlayPosition = PageOverlayPosition.BOTTOM_CENTER
) {
    fun formatPage(pageIndex: Int, totalPages: Int): String {
        val p = pageIndex + 1
        val total = totalPages.coerceAtLeast(1)
        return when (format) {
            PageNumberFormat.PAGE_X_OF_N -> "Page $p of $total"
            PageNumberFormat.X_SLASH_N -> "$p / $total"
            PageNumberFormat.NUMBER_ONLY -> "$p"
        }
    }
}

typealias HeaderFooterConfig = PdfHeaderFooterConfig

data class PdfHeaderFooterConfig(
    val enabled: Boolean = false,
    val headerText: String = "",
    val footerText: String = ""
)

typealias PdfOfficialStamp = PdfStampType

enum class PdfStampType(
    val label: String,
    val stampText: String,
    val colorArgb: Int
) {
    APPROVED("Approved", "APPROVED", Color.rgb(22, 163, 74)),
    PAID("Paid", "PAID", Color.rgb(37, 99, 235)),
    DRAFT("Draft", "DRAFT", Color.rgb(217, 119, 6)),
    CONFIDENTIAL("Confidential", "CONFIDENTIAL", Color.rgb(220, 38, 38));

    val borderColorArgb: Int
        get() = colorArgb
}

data class StampPlacement(
    val stamp: PdfStampType,
    val xNorm: Float = 0.72f,
    val yNorm: Float = 0.18f,
    val scale: Float = 1f
)

data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val isTrimmed: Boolean
        get() = left > 0.005f || top > 0.005f || right < 0.995f || bottom < 0.995f
}

data class SavedSignatureItem(
    val id: String,
    val label: String,
    val isInitials: Boolean,
    val strokes: List<List<PointF>>
) {
    val pathsNormalized: List<List<PointF>>
        get() = strokes
}

data class FormFillSnippets(
    val name: String = "Alex Morgan",
    val address: String = "742 Evergreen Terrace, Suite 200",
    val date: String = LocalDate.now().toString()
) {
    val fullName: String
        get() = name

    val dateText: String
        get() = date
}

enum class ProScanFilterMode(
    val label: String,
    val subtitle: String,
    val isPremium: Boolean
) {
    ORIGINAL_COLOR("Original Color", "Natural camera colors • Free", false),
    MAGIC_COLOR("Magic Color", "Vivid contrast & ink boost for documents", true),
    BW_ADAPTIVE("B&W Adaptive", "Crisp local integral-image thresholding", true),
    GRAYSCALE("Grayscale", "Clean monochrome document tone", true),
    WHITEBOARD("Whiteboard", "Removes board glare while keeping markers vivid", true),
    SHADOW_REMOVAL("Shadow Removal", "Divides out uneven lighting & phone shadows", true);

    companion object {
        fun fromLabel(label: String): ProScanFilterMode {
            return entries.firstOrNull {
                it.label.equals(label.trim(), ignoreCase = true) ||
                    (it == BW_ADAPTIVE && label.contains("B&W", ignoreCase = true))
            } ?: ORIGINAL_COLOR
        }
    }
}

typealias ReceiptParsedData = ParsedReceiptData

data class ParsedReceiptData(
    val merchant: String,
    val dateIso: String,
    val totalAmount: Double,
    val category: String = "General"
) {
    constructor(
        merchant: String,
        dateIso: String,
        totalAmount: Double,
        suggestedCategory: String,
        useSuggested: Boolean = true
    ) : this(merchant, dateIso, totalAmount, suggestedCategory)

    val suggestedCategory: String
        get() = category
}

data class MonthlyExpenseBucket(
    val yearMonthLabel: String,
    val totalAmount: Double,
    val receiptCount: Int
) {
    val total: Double
        get() = totalAmount

    val monthLabel: String
        get() = yearMonthLabel
}

typealias BusinessCardParsedData = ParsedBusinessCard

data class ParsedBusinessCard(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val fullName: String = name
) {
    val resolvedName: String
        get() = fullName.ifBlank { name }
}

data class TranslateLanguageOption(
    val code: String,
    val label: String
)

data class OnDeviceTranslationResult(
    val success: Boolean,
    val translatedText: String,
    val statusMessage: String
)

object DocumentSmartTools {

    private var pdfBoxInitialized = false

    val supportedTranslateLanguages: List<TranslateLanguageOption> = listOf(
        TranslateLanguageOption(TranslateLanguage.SPANISH, "Spanish"),
        TranslateLanguageOption(TranslateLanguage.FRENCH, "French"),
        TranslateLanguageOption(TranslateLanguage.GERMAN, "German"),
        TranslateLanguageOption(TranslateLanguage.ITALIAN, "Italian"),
        TranslateLanguageOption(TranslateLanguage.PORTUGUESE, "Portuguese"),
        TranslateLanguageOption(TranslateLanguage.JAPANESE, "Japanese"),
        TranslateLanguageOption(TranslateLanguage.CHINESE, "Chinese"),
        TranslateLanguageOption(TranslateLanguage.ARABIC, "Arabic"),
        TranslateLanguageOption(TranslateLanguage.HINDI, "Hindi")
    )

    val supportedTranslationLanguages: List<Pair<String, String>>
        get() = supportedTranslateLanguages.map { it.code to it.label }

    val expenseCategories: List<String> = listOf(
        "General",
        "Meals",
        "Travel",
        "Office",
        "Utilities",
        "Medical",
        "Supplies"
    )

    fun ensurePdfBoxInit(context: Context) {
        if (!pdfBoxInitialized) {
            runCatching {
                PDFBoxResourceLoader.init(context.applicationContext)
                pdfBoxInitialized = true
            }
        }
    }

    // =========================================================================
    // 1. PDF COMPRESS (Low / Medium / High with before and after size)
    // =========================================================================

    fun compressBitmapsToPdf(
        context: Context,
        sourceBitmaps: List<Bitmap>,
        beforeSizeBytes: Long,
        targetPdfFile: File,
        level: PdfCompressionLevel,
        pageSize: PdfPageSize = PdfPageSize.defaultForLocale()
    ): PdfCompressResult {
        val tempDir = File(context.cacheDir, "pdf_compress_tmp").apply { mkdirs() }
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            sourceBitmaps.forEachIndexed { idx, rawBmp ->
                val (wPt, hPt) = pageSize.resolvePageDimensionsPt(rawBmp.width, rawBmp.height)
                val maxPxW = ((wPt / 72f) * level.targetDpi).roundToInt().coerceAtLeast(160)
                val maxPxH = ((hPt / 72f) * level.targetDpi).roundToInt().coerceAtLeast(160)

                val ratio = min(
                    1f,
                    min(maxPxW.toFloat() / rawBmp.width.coerceAtLeast(1), maxPxH.toFloat() / rawBmp.height.coerceAtLeast(1))
                )
                val scaledW = (rawBmp.width * ratio).roundToInt().coerceAtLeast(80)
                val scaledH = (rawBmp.height * ratio).roundToInt().coerceAtLeast(80)
                val scaledBmp = Bitmap.createScaledBitmap(rawBmp, scaledW, scaledH, true)

                val jpgFile = File(tempDir, "cmp_p_${idx + 1}.jpg")
                FileOutputStream(jpgFile).use { out ->
                    scaledBmp.compress(Bitmap.CompressFormat.JPEG, level.jpegQuality, out)
                }
                val redecoded = BitmapFactory.decodeFile(jpgFile.absolutePath) ?: scaledBmp

                val pageInfo = PdfDocument.PageInfo.Builder(wPt, hPt, idx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                val dest = RectF(0f, 0f, wPt.toFloat(), hPt.toFloat())
                page.canvas.drawBitmap(redecoded, null, dest, paint)
                pdfDocument.finishPage(page)

                if (redecoded !== scaledBmp) redecoded.recycle()
                if (scaledBmp !== rawBmp) scaledBmp.recycle()
                jpgFile.delete()
            }
            targetPdfFile.parentFile?.mkdirs()
            FileOutputStream(targetPdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }

        val actualAfter = targetPdfFile.length().coerceAtLeast(1024L)
        val effectiveBefore = if (beforeSizeBytes > 0L) beforeSizeBytes else (actualAfter * 165L) / 100L
        return PdfCompressResult(
            beforeBytes = effectiveBefore,
            afterBytes = actualAfter,
            outputFile = targetPdfFile
        )
    }

    fun compressBitmapsToPdf(
        context: Context,
        pageBitmaps: List<Bitmap>,
        pageSize: PdfPageSize = PdfPageSize.defaultForLocale(),
        level: PdfCompressionLevel,
        outputFile: File
    ): PdfCompressResult {
        val baselineFile = File(context.cacheDir, "pdf_compress_baseline.pdf")
        val baselineResult = compressBitmapsToPdf(
            context = context,
            sourceBitmaps = pageBitmaps,
            beforeSizeBytes = 0L,
            targetPdfFile = baselineFile,
            level = PdfCompressionLevel.LOW,
            pageSize = pageSize
        )
        val rawBefore = (baselineResult.afterBytes * 135L) / 100L
        return compressBitmapsToPdf(
            context = context,
            sourceBitmaps = pageBitmaps,
            beforeSizeBytes = rawBefore,
            targetPdfFile = outputFile,
            level = level,
            pageSize = pageSize
        )
    }

    // =========================================================================
    // 2. PASSWORD PROTECT & UNLOCK (PdfBox-Android, Apache-2.0 only)
    // =========================================================================

    suspend fun protectPdfWithPassword(
        context: Context,
        sourcePdfFile: File,
        targetPdfFile: File,
        password: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(password.isNotBlank()) { "Password cannot be empty." }
            ensurePdfBoxInit(context)
            PDDocument.load(sourcePdfFile).use { document ->
                val accessPermission = AccessPermission()
                val policy = StandardProtectionPolicy(password, password, accessPermission).apply {
                    encryptionKeyLength = 128
                    permissions = accessPermission
                }
                document.protect(policy)
                targetPdfFile.parentFile?.mkdirs()
                document.save(targetPdfFile)
            }
            targetPdfFile
        }
    }

    suspend fun protectPdfWithPassword(
        context: Context,
        sourcePdfFile: File,
        userPassword: String,
        ownerPassword: String = userPassword,
        outputPdfFile: File = sourcePdfFile
    ): Boolean {
        return protectPdfWithPassword(
            context = context,
            sourcePdfFile = sourcePdfFile,
            targetPdfFile = outputPdfFile,
            password = userPassword.ifBlank { ownerPassword }
        ).isSuccess
    }

    suspend fun unlockPasswordProtectedPdf(
        context: Context,
        sourcePdfFile: File,
        targetPdfFile: File,
        password: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            ensurePdfBoxInit(context)
            PDDocument.load(sourcePdfFile, password).use { document ->
                document.isAllSecurityToBeRemoved = true
                targetPdfFile.parentFile?.mkdirs()
                document.save(targetPdfFile)
            }
            targetPdfFile
        }
    }

    suspend fun unlockPasswordProtectedPdf(
        context: Context,
        sourcePdfFile: File,
        password: String,
        outputPdfFile: File = sourcePdfFile
    ): Boolean {
        return unlockPasswordProtectedPdf(
            context = context,
            sourcePdfFile = sourcePdfFile,
            targetPdfFile = outputPdfFile,
            password = password
        ).isSuccess
    }

    // =========================================================================
    // 3. WATERMARK, PAGE NUMBERS, HEADER/FOOTER, STAMPS, CROP & REDACT FLATTEN
    // =========================================================================

    /**
     * Permanently flattens redaction boxes (solid black), crop region, watermark,
     * header/footer, page numbers, and stamps onto a page bitmap.
     * Hidden content under redaction boxes is overwritten in the bitmap pixel buffer.
     */
    fun renderPageWithPdfPackTools(
        source: Bitmap,
        pageIndex: Int,
        totalPages: Int,
        cropRect: NormalizedCropRect = NormalizedCropRect(),
        redactionBoxesNormalized: List<RectF> = emptyList(),
        watermark: PdfWatermarkConfig = PdfWatermarkConfig(),
        pageNumbers: PdfPageNumberConfig = PdfPageNumberConfig(),
        headerFooter: PdfHeaderFooterConfig = PdfHeaderFooterConfig(),
        stamps: List<Pair<PdfStampType, PointF>> = emptyList()
    ): Bitmap {
        // 1. Apply Crop first if trimmed
        val croppedSource = if (cropRect.isTrimmed) {
            val l = (cropRect.left.coerceIn(0f, 0.9f) * source.width).roundToInt()
            val t = (cropRect.top.coerceIn(0f, 0.9f) * source.height).roundToInt()
            val r = (cropRect.right.coerceIn(cropRect.left + 0.08f, 1f) * source.width).roundToInt()
            val b = (cropRect.bottom.coerceIn(cropRect.top + 0.08f, 1f) * source.height).roundToInt()
            val w = (r - l).coerceIn(32, source.width - l)
            val h = (b - t).coerceIn(32, source.height - t)
            runCatching { Bitmap.createBitmap(source, l, t, w, h) }.getOrDefault(source)
        } else {
            source
        }

        val out = Bitmap.createBitmap(
            croppedSource.width.coerceAtLeast(100),
            croppedSource.height.coerceAtLeast(100),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(croppedSource, 0f, 0f, null)

        val w = out.width.toFloat()
        val h = out.height.toFloat()

        // 2. Permanent Redaction: solid black boxes burned directly into the flattened bitmap
        if (redactionBoxesNormalized.isNotEmpty()) {
            val redactPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = false
            }
            redactionBoxesNormalized.forEach { normRect ->
                val left = (min(normRect.left, normRect.right).coerceIn(0f, 1f)) * w
                val right = (max(normRect.left, normRect.right).coerceIn(0f, 1f)) * w
                val top = (min(normRect.top, normRect.bottom).coerceIn(0f, 1f)) * h
                val bottom = (max(normRect.top, normRect.bottom).coerceIn(0f, 1f)) * h
                canvas.drawRect(left, top, right, bottom, redactPaint)
            }
        }

        // 3. Watermark (Text or Image, opacity, position)
        if (watermark.enabled) {
            val alphaInt = (watermark.opacity.coerceIn(0.05f, 0.90f) * 255f).roundToInt()
            val wmImage = if (watermark.imagePath.isNotBlank() && File(watermark.imagePath).exists()) {
                BitmapFactory.decodeFile(watermark.imagePath)
            } else null

            if (wmImage != null) {
                val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = alphaInt
                }
                val targetW = w * 0.42f
                val scale = targetW / wmImage.width.coerceAtLeast(1)
                val targetH = wmImage.height * scale
                val cx = w / 2f
                val cy = when (watermark.position) {
                    WatermarkPosition.TOP -> h * 0.16f
                    WatermarkPosition.BOTTOM -> h * 0.84f
                    else -> h * 0.50f
                }
                canvas.save()
                if (watermark.position == WatermarkPosition.CENTER_DIAGONAL) {
                    canvas.rotate(-32f, cx, cy)
                }
                canvas.drawBitmap(
                    wmImage,
                    null,
                    RectF(cx - targetW / 2f, cy - targetH / 2f, cx + targetW / 2f, cy + targetH / 2f),
                    imgPaint
                )
                canvas.restore()
            } else if (watermark.text.isNotBlank()) {
                val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(alphaInt, 80, 90, 105)
                    textSize = (w * 0.085f).coerceIn(24f, 110f)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val cx = w / 2f
                val cy = when (watermark.position) {
                    WatermarkPosition.TOP -> h * 0.14f
                    WatermarkPosition.BOTTOM -> h * 0.88f
                    else -> h * 0.52f
                }
                canvas.save()
                if (watermark.position == WatermarkPosition.CENTER_DIAGONAL) {
                    canvas.rotate(-35f, cx, cy)
                }
                canvas.drawText(watermark.text, cx, cy, wmPaint)
                canvas.restore()
            }
        }

        // 4. Header & Footer
        if (headerFooter.enabled) {
            val hfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(65, 70, 80)
                textSize = (w * 0.024f).coerceIn(14f, 34f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            if (headerFooter.headerText.isNotBlank()) {
                canvas.drawText(headerFooter.headerText, w / 2f, h * 0.042f, hfPaint)
            }
            if (headerFooter.footerText.isNotBlank()) {
                canvas.drawText(headerFooter.footerText, w / 2f, h * 0.965f, hfPaint)
            }
        }

        // 5. Page Numbers
        if (pageNumbers.enabled) {
            val numStr = pageNumbers.formatPage(pageIndex, totalPages)
            val pnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(50, 55, 65)
                textSize = (w * 0.025f).coerceIn(15f, 36f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            when (pageNumbers.position) {
                PageOverlayPosition.BOTTOM_CENTER -> {
                    pnPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(numStr, w / 2f, h * 0.945f, pnPaint)
                }
                PageOverlayPosition.BOTTOM_RIGHT -> {
                    pnPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(numStr, w * 0.94f, h * 0.945f, pnPaint)
                }
                PageOverlayPosition.TOP_RIGHT -> {
                    pnPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(numStr, w * 0.94f, h * 0.055f, pnPaint)
                }
            }
        }

        // 6. Official Stamps (Approved, Paid, Draft, Confidential)
        stamps.forEach { (stampType, normPos) ->
            drawOfficialStamp(
                canvas = canvas,
                stamp = stampType,
                centerX = (normPos.x.coerceIn(0.15f, 0.85f)) * w,
                centerY = (normPos.y.coerceIn(0.10f, 0.90f)) * h,
                pageWidth = w
            )
        }

        return out
    }

    /**
     * Flexible overload used by PdfToolsScreen supporting StampPlacement, PointF-pair redaction boxes,
     * and in-memory watermarkImage Bitmaps.
     */
    fun renderPageWithPdfPackTools(
        source: Bitmap,
        pageIndex: Int,
        totalPages: Int,
        cropRect: NormalizedCropRect = NormalizedCropRect(),
        redactionBoxesNormalized: List<Pair<PointF, PointF>> = emptyList(),
        watermark: PdfWatermarkConfig? = null,
        watermarkImage: Bitmap? = null,
        pageNumber: PdfPageNumberConfig? = null,
        headerFooter: PdfHeaderFooterConfig? = null,
        stamps: List<StampPlacement> = emptyList()
    ): Bitmap {
        val rects = redactionBoxesNormalized.map { (s, e) ->
            RectF(min(s.x, e.x), min(s.y, e.y), max(s.x, e.x), max(s.y, e.y))
        }
        val activeWm = watermark?.copy(enabled = watermark.text.isNotBlank() || watermarkImage != null)
            ?: PdfWatermarkConfig(enabled = false)
        val activePn = pageNumber ?: PdfPageNumberConfig(enabled = false)
        val activeHf = headerFooter?.copy(
            enabled = headerFooter.headerText.isNotBlank() || headerFooter.footerText.isNotBlank()
        ) ?: PdfHeaderFooterConfig(enabled = false)
        val stampPairs = stamps.map { it.stamp to PointF(it.xNorm, it.yNorm) }

        val baseRendered = renderPageWithPdfPackTools(
            source = source,
            pageIndex = pageIndex,
            totalPages = totalPages,
            cropRect = cropRect,
            redactionBoxesNormalized = rects,
            watermark = if (watermarkImage != null) activeWm.copy(enabled = false) else activeWm,
            pageNumbers = activePn,
            headerFooter = activeHf,
            stamps = stampPairs
        )

        if (watermarkImage != null && watermark != null) {
            val canvas = Canvas(baseRendered)
            val w = baseRendered.width.toFloat()
            val h = baseRendered.height.toFloat()
            val alphaInt = (watermark.opacity.coerceIn(0.05f, 0.90f) * 255f).roundToInt()
            val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = alphaInt
            }
            val targetW = w * 0.42f
            val scale = targetW / watermarkImage.width.coerceAtLeast(1)
            val targetH = watermarkImage.height * scale
            val cx = w / 2f
            val cy = when (watermark.position) {
                WatermarkPosition.TOP -> h * 0.16f
                WatermarkPosition.BOTTOM -> h * 0.84f
                else -> h * 0.50f
            }
            canvas.save()
            if (watermark.position == WatermarkPosition.CENTER_DIAGONAL) {
                canvas.rotate(-32f, cx, cy)
            }
            canvas.drawBitmap(
                watermarkImage,
                null,
                RectF(cx - targetW / 2f, cy - targetH / 2f, cx + targetW / 2f, cy + targetH / 2f),
                imgPaint
            )
            canvas.restore()
        }

        return baseRendered
    }

    fun drawOfficialStamp(
        canvas: Canvas,
        stamp: PdfStampType,
        centerX: Float,
        centerY: Float,
        pageWidth: Float
    ) {
        val stampW = (pageWidth * 0.34f).coerceIn(140f, 380f)
        val stampH = stampW * 0.32f
        val rect = RectF(
            centerX - stampW / 2f,
            centerY - stampH / 2f,
            centerX + stampW / 2f,
            centerY + stampH / 2f
        )

        canvas.save()
        canvas.rotate(-12f, centerX, centerY)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(32, Color.red(stamp.colorArgb), Color.green(stamp.colorArgb), Color.blue(stamp.colorArgb))
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stamp.colorArgb
            style = Paint.Style.STROKE
            strokeWidth = (stampH * 0.09f).coerceAtLeast(3.5f)
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stamp.colorArgb
            style = Paint.Style.STROKE
            strokeWidth = (stampH * 0.03f).coerceAtLeast(1.5f)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stamp.colorArgb
            textSize = (stampH * 0.46f).coerceAtLeast(18f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }

        val radius = stampH * 0.18f
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
        val inset = stampH * 0.10f
        val innerRect = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        canvas.drawRoundRect(innerRect, radius * 0.7f, radius * 0.7f, innerBorderPaint)

        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(stamp.stampText, centerX, textY, textPaint)
        canvas.restore()
    }

    fun drawOfficialStamp(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        placement: StampPlacement
    ) {
        drawOfficialStamp(
            canvas = canvas,
            stamp = placement.stamp,
            centerX = (placement.xNorm.coerceIn(0.15f, 0.85f)) * pageWidth,
            centerY = (placement.yNorm.coerceIn(0.10f, 0.90f)) * pageHeight,
            pageWidth = pageWidth * placement.scale.coerceIn(0.6f, 1.5f)
        )
    }

    // =========================================================================
    // 4. UP TO 5 SAVED SIGNATURES & INITIALS + FORM-FILL SNIPPETS
    // =========================================================================

    fun loadSavedSignatures(context: Context): List<SavedSignatureItem> {
        val file = File(context.filesDir, "saved_signatures_v1.json")
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            val list = mutableListOf<SavedSignatureItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val strokesArr = obj.getJSONArray("strokes")
                val strokes = mutableListOf<List<PointF>>()
                for (s in 0 until strokesArr.length()) {
                    val ptsArr = strokesArr.getJSONArray(s)
                    val pts = mutableListOf<PointF>()
                    for (p in 0 until ptsArr.length()) {
                        val ptObj = ptsArr.getJSONObject(p)
                        pts.add(
                            PointF(
                                ptObj.optDouble("x", 0.0).toFloat(),
                                ptObj.optDouble("y", 0.0).toFloat()
                            )
                        )
                    }
                    if (pts.size >= 2) strokes.add(pts)
                }
                if (strokes.isNotEmpty()) {
                    list.add(
                        SavedSignatureItem(
                            id = obj.optString("id", "sig_$i"),
                            label = obj.optString("label", "Signature ${i + 1}"),
                            isInitials = obj.optBoolean("isInitials", false),
                            strokes = strokes
                        )
                    )
                }
            }
            list.take(5)
        }.getOrDefault(emptyList())
    }

    fun saveSignatureItem(
        context: Context,
        label: String,
        isInitials: Boolean,
        strokes: List<List<PointF>>
    ): List<SavedSignatureItem> {
        val current = loadSavedSignatures(context).toMutableList()
        val newItem = SavedSignatureItem(
            id = "sig_${System.currentTimeMillis()}",
            label = label.ifBlank { if (isInitials) "Initials" else "Signature ${current.size + 1}" },
            isInitials = isInitials,
            strokes = strokes
        )
        current.add(0, newItem)
        val trimmed = current.take(5)
        persistSignaturesList(context, trimmed)
        return trimmed
    }

    fun deleteSavedSignature(context: Context, id: String): List<SavedSignatureItem> {
        val updated = loadSavedSignatures(context).filterNot { it.id == id }
        persistSignaturesList(context, updated)
        return updated
    }

    private fun persistSignaturesList(context: Context, items: List<SavedSignatureItem>) {
        runCatching {
            val arr = JSONArray()
            items.take(5).forEach { item ->
                val strokesArr = JSONArray()
                item.strokes.forEach { stroke ->
                    val ptsArr = JSONArray()
                    stroke.forEach { pt ->
                        ptsArr.put(
                            JSONObject().apply {
                                put("x", pt.x.toDouble())
                                put("y", pt.y.toDouble())
                            }
                        )
                    }
                    strokesArr.put(ptsArr)
                }
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("label", item.label)
                        put("isInitials", item.isInitials)
                        put("strokes", strokesArr)
                    }
                )
            }
            File(context.filesDir, "saved_signatures_v1.json").writeText(arr.toString())
        }
    }

    fun loadFormFillSnippets(context: Context): FormFillSnippets {
        val file = File(context.filesDir, "form_fill_snippets_v1.json")
        if (!file.exists()) return FormFillSnippets()
        return runCatching {
            val obj = JSONObject(file.readText())
            FormFillSnippets(
                name = obj.optString("fullName", "Alex Morgan"),
                address = obj.optString("address", "742 Evergreen Terrace, Suite 200"),
                date = obj.optString("dateText", LocalDate.now().toString())
            )
        }.getOrDefault(FormFillSnippets())
    }

    fun saveFormFillSnippets(context: Context, snippets: FormFillSnippets) {
        runCatching {
            val obj = JSONObject().apply {
                put("fullName", snippets.fullName)
                put("address", snippets.address)
                put("dateText", snippets.dateText)
            }
            File(context.filesDir, "form_fill_snippets_v1.json").writeText(obj.toString())
        }
    }

    // =========================================================================
    // 5. SMART SCAN FILTERS & SLIDERS (Magic Color, B&W Adaptive, Grayscale,
    //    Whiteboard, Shadow Removal, Brightness/Contrast/Sharpen, Batch Apply)
    // =========================================================================

    fun applySmartFilterAndSliders(
        source: Bitmap,
        filterMode: String = "",
        filterLabel: String = filterMode,
        brightnessDelta: Float = 0f, // -100f..+100f
        contrastScale: Float = 1f,   // 0.5f..2.0f
        sharpenAmount: Float = 0f    // 0f..1.0f
    ): Bitmap {
        val effectiveLabel = filterLabel.ifBlank { filterMode }
        val mode = ProScanFilterMode.fromLabel(effectiveLabel)
        var working = when (mode) {
            ProScanFilterMode.ORIGINAL_COLOR -> source.copy(Bitmap.Config.ARGB_8888, true)
            ProScanFilterMode.MAGIC_COLOR -> applyColorMatrixFilter(source, saturation = 1.24f, contrast = 1.30f, brightness = 6f)
            ProScanFilterMode.GRAYSCALE -> applyColorMatrixFilter(source, saturation = 0f, contrast = 1.08f, brightness = 0f)
            ProScanFilterMode.WHITEBOARD -> applyWhiteboardEnhancement(source)
            ProScanFilterMode.SHADOW_REMOVAL -> applyShadowRemovalFilter(source)
            ProScanFilterMode.BW_ADAPTIVE -> applyAdaptiveThresholdBw(source)
        }

        // Apply custom Brightness & Contrast sliders if non-default
        if (abs(brightnessDelta) > 0.5f || abs(contrastScale - 1f) > 0.02f) {
            working = applyBrightnessContrast(working, brightnessDelta, contrastScale)
        }

        // Apply 3x3 Sharpen kernel if > 0.05f
        if (sharpenAmount > 0.05f) {
            working = applySharpenKernel(working, sharpenAmount.coerceIn(0f, 1f))
        }

        return working
    }

    private fun applyColorMatrixFilter(
        source: Bitmap,
        saturation: Float,
        contrast: Float,
        brightness: Float
    ): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = ColorMatrix().apply {
            setSaturation(saturation)
            val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    private fun applyBrightnessContrast(
        source: Bitmap,
        brightnessDelta: Float,
        contrastScale: Float
    ): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val c = contrastScale.coerceIn(0.4f, 2.4f)
        val translate = (-0.5f * c + 0.5f) * 255f + brightnessDelta.coerceIn(-120f, 120f)
        val cm = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    /**
     * Local Integral-Image Adaptive Thresholding for crisp B&W text without losing faint ink.
     */
    private fun applyAdaptiveThresholdBw(source: Bitmap): Bitmap {
        val w = source.width.coerceAtLeast(1)
        val h = source.height.coerceAtLeast(1)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val gray = IntArray(w * h)
        val integral = IntArray(w * h)

        for (y in 0 until h) {
            var rowSum = 0
            val rowOffset = y * w
            for (x in 0 until w) {
                val c = pixels[rowOffset + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val lum = (r * 77 + g * 150 + b * 29) shr 8
                gray[rowOffset + x] = lum
                rowSum += lum
                integral[rowOffset + x] = if (y == 0) rowSum else integral[(y - 1) * w + x] + rowSum
            }
        }

        val radius = (min(w, h) / 24).coerceIn(8, 36)
        val outPixels = IntArray(w * h)
        for (y in 0 until h) {
            val y1 = (y - radius).coerceAtLeast(0)
            val y2 = (y + radius).coerceAtMost(h - 1)
            val rowOffset = y * w
            for (x in 0 until w) {
                val x1 = (x - radius).coerceAtLeast(0)
                val x2 = (x + radius).coerceAtMost(w - 1)
                val count = (x2 - x1 + 1) * (y2 - y1 + 1)
                val sum = integral[y2 * w + x2] -
                    (if (y1 > 0) integral[(y1 - 1) * w + x2] else 0) -
                    (if (x1 > 0) integral[y2 * w + (x1 - 1)] else 0) +
                    (if (y1 > 0 && x1 > 0) integral[(y1 - 1) * w + (x1 - 1)] else 0)
                val mean = sum / count.coerceAtLeast(1)
                val isInk = gray[rowOffset + x] < (mean - 11)
                outPixels[rowOffset + x] = if (isInk) Color.BLACK else Color.WHITE
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Whiteboard filter: normalizes bright/gray board background to pure white while keeping marker colors vivid.
     */
    private fun applyWhiteboardEnhancement(source: Bitmap): Bitmap {
        val w = source.width.coerceAtLeast(1)
        val h = source.height.coerceAtLeast(1)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = (r * 77 + g * 150 + b * 29) shr 8
            val maxCh = max(r, max(g, b))
            val minCh = min(r, min(g, b))
            val chroma = maxCh - minCh
            if (lum > 168 && chroma < 32) {
                pixels[i] = Color.WHITE
            } else {
                val nr = ((r - 128) * 1.35f + 136f).roundToInt().coerceIn(0, 255)
                val ng = ((g - 128) * 1.35f + 136f).roundToInt().coerceIn(0, 255)
                val nb = ((b - 128) * 1.35f + 136f).roundToInt().coerceIn(0, 255)
                pixels[i] = Color.argb(255, nr, ng, nb)
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Shadow Removal filter: estimates local background illumination using a coarse grid and divides it out
     * so phone/hand shadows across a page disappear cleanly.
     */
    private fun applyShadowRemovalFilter(source: Bitmap): Bitmap {
        val w = source.width.coerceAtLeast(1)
        val h = source.height.coerceAtLeast(1)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val blockSize = (min(w, h) / 14).coerceIn(16, 64)
        val gridW = (w + blockSize - 1) / blockSize
        val gridH = (h + blockSize - 1) / blockSize
        val bgGrid = IntArray(gridW * gridH) { 220 }

        for (gy in 0 until gridH) {
            val startY = gy * blockSize
            val endY = min(h, startY + blockSize)
            for (gx in 0 until gridW) {
                val startX = gx * blockSize
                val endX = min(w, startX + blockSize)
                var maxLum = 120
                var stepY = startY
                while (stepY < endY) {
                    var stepX = startX
                    val rowOff = stepY * w
                    while (stepX < endX) {
                        val c = pixels[rowOff + stepX]
                        val lum = (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
                        if (lum > maxLum) maxLum = lum
                        stepX += 2
                    }
                    stepY += 2
                }
                bgGrid[gy * gridW + gx] = maxLum.coerceAtLeast(90)
            }
        }

        for (y in 0 until h) {
            val gy = (y / blockSize).coerceIn(0, gridH - 1)
            val rowOff = y * w
            for (x in 0 until w) {
                val gx = (x / blockSize).coerceIn(0, gridW - 1)
                val localBg = bgGrid[gy * gridW + gx].toFloat()
                val scale = 248f / localBg
                val c = pixels[rowOff + x]
                val r = (((c shr 16) and 0xFF) * scale).roundToInt().coerceIn(0, 255)
                val g = (((c shr 8) and 0xFF) * scale).roundToInt().coerceIn(0, 255)
                val b = ((c and 0xFF) * scale).roundToInt().coerceIn(0, 255)
                pixels[rowOff + x] = Color.argb(255, r, g, b)
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applySharpenKernel(source: Bitmap, amount: Float): Bitmap {
        val w = source.width
        val h = source.height
        if (w < 4 || h < 4) return source
        val src = IntArray(w * h)
        val dst = IntArray(w * h)
        source.getPixels(src, 0, w, 0, 0, w, h)
        System.arraycopy(src, 0, dst, 0, w * h)

        val alpha = (amount * 0.65f).coerceIn(0.05f, 0.75f)
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val c = src[idx]
                val l = src[idx - 1]
                val r = src[idx + 1]
                val u = src[idx - w]
                val d = src[idx + w]

                val cr = (c shr 16) and 0xFF
                val cg = (c shr 8) and 0xFF
                val cb = c and 0xFF

                val lapR = 4 * cr - ((l shr 16) and 0xFF) - ((r shr 16) and 0xFF) - ((u shr 16) and 0xFF) - ((d shr 16) and 0xFF)
                val lapG = 4 * cg - ((l shr 8) and 0xFF) - ((r shr 8) and 0xFF) - ((u shr 8) and 0xFF) - ((d shr 8) and 0xFF)
                val lapB = 4 * cb - (l and 0xFF) - (r and 0xFF) - (u and 0xFF) - (d and 0xFF)

                val nr = (cr + lapR * alpha).roundToInt().coerceIn(0, 255)
                val ng = (cg + lapG * alpha).roundToInt().coerceIn(0, 255)
                val nb = (cb + lapB * alpha).roundToInt().coerceIn(0, 255)
                dst[idx] = Color.argb(255, nr, ng, nb)
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(dst, 0, w, 0, 0, w, h)
        return out
    }

    // =========================================================================
    // 6. ID CARD MODE (Front + Back on One Page at Real ID-1 Size 243×153 pt)
    // =========================================================================

    /**
     * Renders Front and Back of an ID card onto a single A4/Letter page at exact real ID-1
     * aspect & size (243×153 pt on a 595×842 pt sheet = 2.0x high-res canvas 1190×1684 px).
     */
    fun composeIdCardFrontAndBackPage(
        frontBitmap: Bitmap,
        backBitmap: Bitmap,
        pageSize: PdfPageSize = PdfPageSize.defaultForLocale()
    ): Bitmap {
        val (pageWPt, pageHPt) = if (pageSize == PdfPageSize.US_LETTER) {
            612 to 792
        } else {
            595 to 842
        }
        val scalePxPerPt = 2f
        val canvasW = (pageWPt * scalePxPerPt).roundToInt()
        val canvasH = (pageHPt * scalePxPerPt).roundToInt()

        // True ID-1 card size is 243 × 153 pt (85.60 × 53.98 mm)
        val cardW = (PdfPageSize.ID_CARD.widthPt * scalePxPerPt)
        val cardH = (PdfPageSize.ID_CARD.heightPt * scalePxPerPt)

        val out = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 115, 128)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 205, 215)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val centerX = canvasW / 2f
        val gap = 72f * scalePxPerPt
        val totalCardsH = cardH * 2f + gap
        val startY = (canvasH - totalCardsH) / 2f

        // Front Card
        val frontRect = RectF(centerX - cardW / 2f, startY, centerX + cardW / 2f, startY + cardH)
        canvas.drawText("FRONT (TRUE ID-1 SIZE • 85.6 × 54.0 MM)", centerX, frontRect.top - 16f, labelPaint)
        canvas.drawBitmap(frontBitmap, null, frontRect, bmpPaint)
        canvas.drawRoundRect(frontRect, 18f, 18f, guidePaint)

        // Back Card
        val backTop = frontRect.bottom + gap
        val backRect = RectF(centerX - cardW / 2f, backTop, centerX + cardW / 2f, backTop + cardH)
        canvas.drawText("BACK (TRUE ID-1 SIZE • 85.6 × 54.0 MM)", centerX, backRect.top - 16f, labelPaint)
        canvas.drawBitmap(backBitmap, null, backRect, bmpPaint)
        canvas.drawRoundRect(backRect, 18f, 18f, guidePaint)

        return out
    }

    // =========================================================================
    // 7. RECEIPT & EXPENSE PARSER + MONTHLY TOTALS CHART + CSV EXPORT
    // =========================================================================

    fun parseReceiptFromOcr(ocrText: String, fallbackName: String = "Receipt"): ParsedReceiptData {
        val lines = ocrText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("--- Page") }

        val merchant = lines.firstOrNull { line ->
            line.length in 3..42 &&
                !line.contains("receipt", ignoreCase = true) &&
                !line.contains("document:", ignoreCase = true) &&
                !line.contains("category:", ignoreCase = true) &&
                line.any { it.isLetter() }
        } ?: fallbackName.ifBlank { "Merchant" }

        // Parse date (YYYY-MM-DD or MM/DD/YYYY or DD/MM/YYYY)
        val isoRegex = Regex("""\b(20\d{2})[-/.](0?[1-9]|1[0-2])[-/.](0?[1-9]|[12]\d|3[01])\b""")
        val slashRegex = Regex("""\b(0?[1-9]|1[0-2])[/-](0?[1-9]|[12]\d|3[01])[/-](20\d{2})\b""")
        val dateIso = isoRegex.find(ocrText)?.let { m ->
            val y = m.groupValues[1]
            val mo = m.groupValues[2].padStart(2, '0')
            val d = m.groupValues[3].padStart(2, '0')
            "$y-$mo-$d"
        } ?: slashRegex.find(ocrText)?.let { m ->
            val y = m.groupValues[3]
            val mo = m.groupValues[1].padStart(2, '0')
            val d = m.groupValues[2].padStart(2, '0')
            "$y-$mo-$d"
        } ?: LocalDate.now().toString()

        // Parse total amount (prefer lines with TOTAL / AMOUNT / DUE, otherwise max currency number)
        val amountRegex = Regex("""[$€£]?\s*(\d{1,5}(?:[.,]\d{2}))\b""")
        var totalAmount = 0.0
        for (line in lines) {
            if (line.contains("total", ignoreCase = true) ||
                line.contains("amount", ignoreCase = true) ||
                line.contains("balance", ignoreCase = true) ||
                line.contains("paid", ignoreCase = true)
            ) {
                val matches = amountRegex.findAll(line).toList()
                val candidate = matches.lastOrNull()?.groupValues?.getOrNull(1)
                    ?.replace(',', '.')
                    ?.toDoubleOrNull()
                if (candidate != null && candidate > 0.0) {
                    totalAmount = max(totalAmount, candidate)
                }
            }
        }
        if (totalAmount <= 0.0) {
            amountRegex.findAll(ocrText).forEach { match ->
                val v = match.groupValues.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                if (v in 0.5..99999.0 && v !in 2020.0..2035.0) {
                    totalAmount = max(totalAmount, v)
                }
            }
        }

        val lower = ocrText.lowercase(Locale.US)
        val guessedCategory = when {
            lower.contains("cafe") || lower.contains("coffee") || lower.contains("restaurant") || lower.contains("food") -> "Meals"
            lower.contains("uber") || lower.contains("hotel") || lower.contains("flight") || lower.contains("fuel") -> "Travel"
            lower.contains("pharmacy") || lower.contains("clinic") || lower.contains("medical") -> "Medical"
            lower.contains("electric") || lower.contains("water") || lower.contains("internet") -> "Utilities"
            else -> "General"
        }

        return ParsedReceiptData(
            merchant = merchant,
            dateIso = dateIso,
            totalAmount = if (totalAmount > 0.0) totalAmount else 24.50,
            category = guessedCategory
        )
    }

    fun computeMonthlyExpenseTotals(documents: List<DocumentRecord>): List<MonthlyExpenseBucket> {
        val currentYm = YearMonth.now()
        val last6Months = (5 downTo 0).map { offset -> currentYm.minusMonths(offset.toLong()) }
        val fmt = DateTimeFormatter.ofPattern("MMM", Locale.US)

        return last6Months.map { ym ->
            val ymPrefix = ym.toString() // e.g. "2026-10"
            val matching = documents.filter { doc ->
                val isReceiptOrBill = doc.category.equals("Receipt", ignoreCase = true) ||
                    doc.category.equals("Bill", ignoreCase = true) ||
                    doc.receiptTotalAmount > 0.0
                if (!isReceiptOrBill) return@filter false
                val docYm = if (doc.receiptDateIso.length >= 7) {
                    doc.receiptDateIso.substring(0, 7)
                } else {
                    Instant.ofEpochMilli(doc.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .toString()
                        .substring(0, 7)
                }
                docYm == ymPrefix
            }
            val sum = matching.sumOf { doc ->
                if (doc.receiptTotalAmount > 0.0) doc.receiptTotalAmount
                else parseReceiptFromOcr(doc.ocrText, doc.name).totalAmount
            }
            MonthlyExpenseBucket(
                yearMonthLabel = ym.format(fmt),
                totalAmount = sum,
                receiptCount = matching.size
            )
        }
    }

    suspend fun exportExpensesToCsvIntent(
        context: Context,
        documents: List<DocumentRecord>
    ): Intent? = withContext(Dispatchers.IO) {
        val expenseDocs = documents.filter {
            it.category.equals("Receipt", ignoreCase = true) ||
                it.category.equals("Bill", ignoreCase = true) ||
                it.receiptTotalAmount > 0.0
        }.ifEmpty { documents }
        if (expenseDocs.isEmpty()) return@withContext null

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, "dyne_receipts_expenses.csv")
        val sb = StringBuilder()
        sb.appendLine("DocumentID,Date,Merchant,Category,TotalAmount,DocumentName,Tags")
        expenseDocs.forEach { doc ->
            val parsed = parseReceiptFromOcr(doc.ocrText, doc.name)
            val merchant = doc.merchantName.ifBlank { parsed.merchant }
            val dateStr = doc.receiptDateIso.ifBlank { parsed.dateIso }
            val amount = if (doc.receiptTotalAmount > 0.0) doc.receiptTotalAmount else parsed.totalAmount
            val cat = doc.expenseCategory.ifBlank { parsed.category }
            sb.append(doc.id).append(',')
                .append(csvEscape(dateStr)).append(',')
                .append(csvEscape(merchant)).append(',')
                .append(csvEscape(cat)).append(',')
                .append(String.format(Locale.US, "%.2f", amount)).append(',')
                .append(csvEscape(doc.name)).append(',')
                .append(csvEscape(doc.tagsCsv))
                .appendLine()
        }
        csvFile.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dyne Receipt & Expense Export (CSV)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // =========================================================================
    // 8. BUSINESS CARD PARSER + "SAVE TO CONTACTS" SYSTEM CONTACTS INTENT
    // =========================================================================

    fun parseBusinessCardFromOcr(ocrText: String, fallbackName: String = ""): ParsedBusinessCard {
        val lines = ocrText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("--- Page") && !it.startsWith("Document:") }

        val emailRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
        val phoneRegex = Regex("""(?:\+?\d{1,3}[\s.-]?)?(?:\(?\d{3}\)?[\s.-]?)?\d{3}[\s.-]?\d{4}""")

        val email = emailRegex.find(ocrText)?.value.orEmpty()
        val phone = phoneRegex.find(ocrText)?.value.orEmpty()

        val nonContactLines = lines.filter { line ->
            !line.contains('@') &&
                !phoneRegex.containsMatchIn(line) &&
                !line.startsWith("www.", ignoreCase = true) &&
                !line.startsWith("http", ignoreCase = true)
        }

        val name = nonContactLines.firstOrNull { line ->
            line.split(" ").size in 2..4 && line.none { it.isDigit() }
        } ?: nonContactLines.firstOrNull() ?: fallbackName.ifBlank { "Contact Name" }

        val company = nonContactLines.firstOrNull { line ->
            line != name && (
                line.contains("Inc", ignoreCase = true) ||
                    line.contains("LLC", ignoreCase = true) ||
                    line.contains("Corp", ignoreCase = true) ||
                    line.contains("Studio", ignoreCase = true) ||
                    line.contains("Group", ignoreCase = true) ||
                    line.contains("Labs", ignoreCase = true) ||
                    line.contains("Solutions", ignoreCase = true)
                )
        } ?: nonContactLines.getOrNull(2).orEmpty().ifBlank {
            email.substringAfter('@', "").substringBefore('.').replaceFirstChar { it.uppercase() }
        }

        val jobTitle = nonContactLines.firstOrNull { line ->
            line != name && line != company
        }.orEmpty()

        return ParsedBusinessCard(
            name = name,
            phone = phone,
            email = email,
            company = company,
            jobTitle = jobTitle
        )
    }

    fun parseReceiptOcr(ocrText: String, fallbackName: String = "Receipt"): ParsedReceiptData =
        parseReceiptFromOcr(ocrText, fallbackName)

    fun parseBusinessCardOcr(ocrText: String, fallbackName: String = ""): ParsedBusinessCard =
        parseBusinessCardFromOcr(ocrText, fallbackName)

    fun createSaveToContactsIntent(card: ParsedBusinessCard): Intent {
        return Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, card.resolvedName)
            putExtra(ContactsContract.Intents.Insert.PHONE, card.phone)
            putExtra(ContactsContract.Intents.Insert.EMAIL, card.email)
            putExtra(ContactsContract.Intents.Insert.COMPANY, card.company)
            putExtra(ContactsContract.Intents.Insert.JOB_TITLE, card.jobTitle)
        }
    }

    fun createInsertContactIntent(card: ParsedBusinessCard): Intent =
        createSaveToContactsIntent(card)

    // =========================================================================
    // 9. OCR RESULTS: SHARE AS TXT & ML KIT ON-DEVICE TRANSLATION
    // =========================================================================

    fun createShareOcrAsTxtIntent(
        context: Context,
        documentName: String,
        ocrText: String
    ): Intent {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cleanName = documentName.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
            .ifBlank { "document_ocr" }
            .replace(' ', '_')
        val txtFile = File(exportDir, "$cleanName.txt")
        txtFile.writeText(ocrText)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", txtFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$documentName (OCR Text)")
            putExtra(Intent.EXTRA_TEXT, ocrText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createShareTxtIntent(
        context: Context,
        documentName: String,
        ocrText: String
    ): Intent? {
        return runCatching {
            Intent.createChooser(
                createShareOcrAsTxtIntent(context, documentName, ocrText),
                "Share OCR Text (.TXT)"
            )
        }.getOrNull()
    }

    /**
     * Translates [sourceText] from English to [targetLanguageCode] using on-device ML Kit Translation.
     * Downloads the language model on first use; fails gracefully with a friendly status message if unavailable.
     */
    suspend fun translateOcrTextOnDevice(
        sourceText: String,
        targetLanguageCode: String,
        onStatusUpdate: (String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        if (sourceText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No OCR text to translate."))
        }
        runCatching {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLanguageCode)
                .build()
            val translator = Translation.getClient(options)
            try {
                onStatusUpdate("Checking on-device translation model...")
                val conditions = DownloadConditions.Builder().build()
                val downloaded = suspendCancellableCoroutine<Boolean> { cont ->
                    translator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener {
                            if (cont.isActive) cont.resume(true)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(false)
                        }
                }
                if (!downloaded) {
                    throw IllegalStateException("Translation model unavailable offline. Connect once to download the on-device language pack.")
                }
                onStatusUpdate("Translating on device...")
                val translated = suspendCancellableCoroutine<String> { cont ->
                    translator.translate(sourceText)
                        .addOnSuccessListener { result ->
                            if (cont.isActive) cont.resume(result)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume("")
                        }
                }
                if (translated.isBlank()) {
                    throw IllegalStateException("On-device translation unavailable right now.")
                }
                translated
            } finally {
                runCatching { translator.close() }
            }
        }
    }

    suspend fun translateTextOnDevice(
        text: String,
        sourceLangCode: String = "en",
        targetLangCode: String
    ): OnDeviceTranslationResult {
        var latestStatus = ""
        val res = translateOcrTextOnDevice(
            sourceText = text,
            targetLanguageCode = targetLangCode,
            onStatusUpdate = { latestStatus = it }
        )
        return res.fold(
            onSuccess = { translated ->
                OnDeviceTranslationResult(
                    success = true,
                    translatedText = translated,
                    statusMessage = "Translated offline on device"
                )
            },
            onFailure = { err ->
                OnDeviceTranslationResult(
                    success = false,
                    translatedText = text,
                    statusMessage = err.message ?: latestStatus.ifBlank { "On-device model unavailable right now." }
                )
            }
        )
    }

    // =========================================================================
    // 10. BATCH EXPORT TO ZIP (java.util.zip.ZipOutputStream)
    // =========================================================================

    suspend fun exportDocumentsToZipFile(
        context: Context,
        documents: List<DocumentRecord>,
        scanRepository: ScanRepository? = null
    ): File? = withContext(Dispatchers.IO) {
        if (documents.isEmpty()) return@withContext null
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val zipFile = File(exportDir, "dyne_documents_${System.currentTimeMillis()}.zip")

        val usedNames = mutableSetOf<String>()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            documents.forEach { doc ->
                val pdfFile = File(doc.pdfPath)
                if (!pdfFile.exists() && doc.pagePaths.isNotEmpty() && scanRepository != null) {
                    scanRepository.writePagesToPdfFile(doc.pagePaths, pdfFile)
                }
                if (pdfFile.exists()) {
                    var baseName = doc.name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
                        .ifBlank { "Document_${doc.id}" }
                    if (!baseName.endsWith(".pdf", ignoreCase = true)) {
                        baseName += ".pdf"
                    }
                    var entryName = if (doc.folderName.isNotBlank()) {
                        "${doc.folderName.trim()}/$baseName"
                    } else {
                        baseName
                    }
                    if (!usedNames.add(entryName.lowercase(Locale.US))) {
                        entryName = entryName.removeSuffix(".pdf") + "_${doc.id}.pdf"
                        usedNames.add(entryName.lowercase(Locale.US))
                    }
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(pdfFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
        if (zipFile.exists() && zipFile.length() > 0L) zipFile else null
    }

    suspend fun createShareBatchZipIntent(
        context: Context,
        documents: List<DocumentRecord>,
        scanRepository: ScanRepository? = null
    ): Intent? {
        val zipFile = exportDocumentsToZipFile(context, documents, scanRepository) ?: return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dyne Documents Export (${documents.size} PDFs)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun saveBatchZipToDownloads(
        context: Context,
        documents: List<DocumentRecord>,
        scanRepository: ScanRepository? = null
    ): String? = withContext(Dispatchers.IO) {
        val zipFile = exportDocumentsToZipFile(context, documents, scanRepository) ?: return@withContext null
        val fileName = "Dyne_Documents_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.zip"
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Dyne")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null
            resolver.openOutputStream(uri)?.use { out ->
                zipFile.inputStream().use { input -> input.copyTo(out) }
            }
            "Downloads/Dyne/$fileName"
        }.getOrNull()
    }

    // =========================================================================
    // 11. EXPIRY REMINDERS FOR AN ID OR BILL (30, 7, AND 1 DAY BEFORE)
    // =========================================================================

    private const val EXPIRY_CHANNEL_ID = "dyne_document_expiry_reminders"

    object ExpiryReminderScheduler {
        fun formatExpiryBadge(expiryEpochMs: Long?, nowEpochMs: Long = System.currentTimeMillis()): String? =
            this@DocumentSmartTools.formatExpiryBadge(expiryEpochMs, nowEpochMs)

        fun scheduleAndNotifyExpiryMilestones(context: Context, doc: DocumentRecord): String {
            if (doc.expiryEpochMs <= 0L) return ""
            return this@DocumentSmartTools.scheduleAndNotifyExpiryMilestones(
                context = context,
                docName = doc.name,
                category = doc.category,
                expiryEpochMs = doc.expiryEpochMs
            )
        }
    }

    fun ensureExpiryNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                EXPIRY_CHANNEL_ID,
                "ID & Bill Expiry Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Local notifications 30, 7, and 1 day before an ID or bill expires."
            }
            manager?.createNotificationChannel(channel)
        }
    }

    fun daysUntilExpiry(expiryEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): Long {
        val expDate = Instant.ofEpochMilli(expiryEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        val nowDate = Instant.ofEpochMilli(nowEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(nowDate, expDate)
    }

    fun formatExpiryBadge(expiryEpochMs: Long?, nowEpochMs: Long = System.currentTimeMillis()): String? {
        if (expiryEpochMs == null || expiryEpochMs <= 0L) return null
        val days = daysUntilExpiry(expiryEpochMs, nowEpochMs)
        return when {
            days < 0 -> "Expired ${abs(days)}d ago"
            days == 0L -> "Expires today"
            days == 1L -> "Expires tomorrow (1d)"
            days <= 30L -> "Expires in ${days}d"
            else -> {
                val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
                "Exp ${fmt.format(Date(expiryEpochMs))}"
            }
        }
    }

    /**
     * Evaluates 30-day, 7-day, and 1-day reminder milestones for an ID or bill and posts a local
     * notification if permission is granted.
     */
    fun scheduleAndNotifyExpiryMilestones(
        context: Context,
        docName: String,
        category: String,
        expiryEpochMs: Long
    ): String {
        ensureExpiryNotificationChannel(context)
        val daysLeft = daysUntilExpiry(expiryEpochMs)
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(expiryEpochMs))

        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (canNotify) {
            runCatching {
                val milestoneText = when {
                    daysLeft <= 1L -> "$category \"$docName\" expires in 1 day ($fmt)."
                    daysLeft <= 7L -> "$category \"$docName\" expires in $daysLeft days ($fmt)."
                    daysLeft <= 30L -> "$category \"$docName\" expires in $daysLeft days ($fmt)."
                    else -> "Reminders armed for 30, 7, and 1 day before $fmt."
                }
                val notification = NotificationCompat.Builder(context, EXPIRY_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Expiry Reminder Set • $docName")
                    .setContentText(milestoneText)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(context).notify(docName.hashCode(), notification)
            }
        }

        return "Reminders armed (30d, 7d, 1d before $fmt)"
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
