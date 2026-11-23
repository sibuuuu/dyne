package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ScanRepository(private val scanDao: ScanDao) {

    val allScans: Flow<List<ScanRecord>> = scanDao.getAllScansFlow()
    val allDocuments: Flow<List<DocumentRecord>> = scanDao.getAllDocumentsFlow()
    val allNotes: Flow<List<NoteRecord>> = scanDao.getAllNotesFlow()
    val allCoinLedger: Flow<List<CoinLedgerRecord>> = scanDao.getAllCoinLedgerFlow()
    val allMediaRecentFiles: Flow<List<MediaRecentFileRecord>> = scanDao.getAllMediaRecentFilesFlow()

    suspend fun recordMediaRecentFile(
        fileName: String,
        toolId: String,
        toolName: String,
        mimeType: String,
        outputUriString: String = "",
        localCachePath: String = "",
        beforeSizeBytes: Long = 0L,
        afterSizeBytes: Long = 0L,
        summaryLabel: String = "",
        timestampMs: Long = System.currentTimeMillis()
    ): MediaRecentFileRecord = withContext(Dispatchers.IO) {
        val entry = MediaRecentFileRecord(
            fileName = fileName,
            toolId = toolId,
            toolName = toolName,
            mimeType = mimeType,
            outputUriString = outputUriString,
            localCachePath = localCachePath,
            beforeSizeBytes = beforeSizeBytes,
            afterSizeBytes = afterSizeBytes,
            summaryLabel = summaryLabel,
            timestamp = timestampMs
        )
        val id = scanDao.insertMediaRecentFile(entry)
        entry.copy(id = id)
    }

    suspend fun deleteMediaRecentFile(id: Long) = withContext(Dispatchers.IO) {
        scanDao.deleteMediaRecentFileById(id)
    }

    suspend fun clearAllMediaRecentFiles() = withContext(Dispatchers.IO) {
        scanDao.deleteAllMediaRecentFiles()
    }

    suspend fun recordCoinTransaction(
        delta: Int,
        balanceAfter: Int,
        reason: String,
        timestampMs: Long = System.currentTimeMillis()
    ): CoinLedgerRecord {
        val entry = CoinLedgerRecord(
            delta = delta,
            balanceAfter = balanceAfter,
            reason = reason,
            timestamp = timestampMs
        )
        val id = scanDao.insertCoinLedger(entry)
        return entry.copy(id = id)
    }

    suspend fun recordScan(
        rawContent: String,
        formatName: String,
        is2dMatrix: Boolean,
        isPro: Boolean
    ): ScanRecord {
        val detectedType = detectContentType(rawContent, is2dMatrix)
        val displayTitle = extractDisplaySummary(rawContent, detectedType)
        val record = ScanRecord(
            rawContent = rawContent,
            displayTitle = displayTitle,
            formatName = formatName,
            is2dMatrix = is2dMatrix,
            contentType = detectedType.name,
            timestamp = System.currentTimeMillis()
        )
        val id = scanDao.insertScan(record)
        if (!isPro) {
            scanDao.trimHistoryToLimit(10)
        }
        return record.copy(id = id)
    }

    suspend fun toggleScanFavorite(record: ScanRecord) {
        scanDao.setScanFavorite(record.id, !record.isFavorite)
    }

    suspend fun deleteScan(id: Long) {
        scanDao.deleteScanById(id)
    }

    /**
     * Saves a multi-page document (from GmsDocumentScanning or CameraX + manual corner crop)
     * into app-private storage (full-res EXIF-corrected original JPEGs + filtered JPEGs + thumbnail +
     * custom multi-page PDF built page-by-page with android.graphics.pdf.PdfDocument) and inserts metadata into Room.
     * Never uses GmsDocumentScanner's own PDF output.
     */
    suspend fun createAndSaveDocument(
        context: Context,
        name: String,
        category: String,
        filterMode: String,
        pageBitmaps: List<Bitmap>,
        pageSize: PdfPageSize = PdfPageSize.defaultForLocale(),
        fitMode: PdfFitMode = PdfFitMode.FIT,
        marginMode: PdfMarginMode = PdfMarginMode.SMALL_10MM,
        qualityDpi: PdfQualityDpi = PdfQualityDpi.HIGH_200,
        pageLimit: Int = 50,
        initialOcrText: String = ""
    ): DocumentRecord = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val baseId = "doc_$now"

        val finalBitmaps = if (pageBitmaps.isNotEmpty()) {
            pageBitmaps.take(pageLimit.coerceIn(1, 50))
        } else {
            listOf(generateSampleDocumentBitmap(name, category, filterMode, 1))
        }

        val savedOriginalPaths = mutableListOf<String>()
        val savedPagePaths = mutableListOf<String>()
        finalBitmaps.forEachIndexed { index, rawBitmap ->
            // Save original full-resolution JPEG (already EXIF rotated on decode) for lossless re-export
            val origFile = File(docsDir, "${baseId}_orig_p${index + 1}.jpg")
            FileOutputStream(origFile).use { out ->
                rawBitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
            }
            savedOriginalPaths.add(origFile.absolutePath)

            val filtered = applyFilterToBitmap(rawBitmap, filterMode)
            val pageFile = File(docsDir, "${baseId}_p${index + 1}.jpg")
            FileOutputStream(pageFile).use { out ->
                filtered.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            savedPagePaths.add(pageFile.absolutePath)
        }

        val thumbFile = File(docsDir, "${baseId}_thumb.jpg")
        val firstBitmap = BitmapFactory.decodeFile(savedPagePaths.first())
            ?: finalBitmaps.first()
        FileOutputStream(thumbFile).use { out ->
            firstBitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
        }

        val pdfFile = File(docsDir, "$baseId.pdf")
        writePagesToPdfFile(
            pagePaths = savedPagePaths,
            targetPdfFile = pdfFile,
            pageSize = pageSize,
            fitMode = fitMode,
            marginMode = marginMode,
            qualityDpi = qualityDpi
        )

        val autoName = name.ifBlank { defaultDocumentName(now) }
        val record = DocumentRecord(
            name = autoName,
            category = category.ifBlank { "Other" },
            createdAt = now,
            pageCount = savedPagePaths.size,
            pdfPath = pdfFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            pagePathsJoined = savedPagePaths.joinToString("|"),
            originalPagePathsJoined = savedOriginalPaths.joinToString("|"),
            pageSizeId = pageSize.id,
            fitModeId = fitMode.id,
            marginModeId = marginMode.id,
            qualityDpiId = qualityDpi.id,
            ocrText = initialOcrText,
            favorite = false,
            locked = false,
            filterMode = filterMode
        )
        val insertedId = scanDao.insertDocument(record)
        record.copy(id = insertedId)
    }

    /**
     * Re-exports an existing document with new Page Size, Fit Mode, Margin, Quality DPI, or Filter
     * directly from the saved full-resolution original JPEGs.
     */
    suspend fun reExportDocumentPdf(
        context: Context,
        doc: DocumentRecord,
        pageSize: PdfPageSize,
        fitMode: PdfFitMode,
        marginMode: PdfMarginMode,
        qualityDpi: PdfQualityDpi,
        filterMode: String = doc.filterMode
    ): DocumentRecord = withContext(Dispatchers.IO) {
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val origPaths = doc.originalPagePaths.filter { File(it).exists() }
        val displayPaths = if (filterMode != doc.filterMode && origPaths.isNotEmpty()) {
            val rebuilt = mutableListOf<String>()
            origPaths.forEachIndexed { idx, origPath ->
                val raw = BitmapFactory.decodeFile(origPath)
                if (raw != null) {
                    val filtered = applyFilterToBitmap(raw, filterMode)
                    val outPath = doc.pagePaths.getOrNull(idx)?.let { File(it) }
                        ?: File(docsDir, "doc_${doc.id}_p${idx + 1}.jpg")
                    FileOutputStream(outPath).use { out ->
                        filtered.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    rebuilt.add(outPath.absolutePath)
                }
            }
            rebuilt.ifEmpty { doc.pagePaths }
        } else {
            doc.pagePaths.filter { File(it).exists() }.ifEmpty { origPaths }
        }

        val pdfFile = if (doc.pdfPath.isNotBlank()) File(doc.pdfPath) else File(docsDir, "doc_${doc.id}.pdf")
        writePagesToPdfFile(
            pagePaths = displayPaths,
            targetPdfFile = pdfFile,
            pageSize = pageSize,
            fitMode = fitMode,
            marginMode = marginMode,
            qualityDpi = qualityDpi
        )

        val updated = doc.copy(
            pagePathsJoined = displayPaths.joinToString("|"),
            pageSizeId = pageSize.id,
            fitModeId = fitMode.id,
            marginModeId = marginMode.id,
            qualityDpiId = qualityDpi.id,
            filterMode = filterMode,
            pdfPath = pdfFile.absolutePath
        )
        scanDao.updateDocument(updated)
        updated
    }

    /**
     * Rebuilds the PDF and thumbnail whenever pages are reordered, deleted, added, or merged.
     */
    suspend fun updateDocumentPages(
        context: Context,
        doc: DocumentRecord,
        newPagePaths: List<String>
    ): DocumentRecord = withContext(Dispatchers.IO) {
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val validPaths = newPagePaths.filter { File(it).exists() }.take(50)
        val resolvedPaths = if (validPaths.isNotEmpty()) {
            validPaths
        } else {
            val fallbackFile = File(docsDir, "doc_${doc.id}_p1.jpg")
            val bmp = generateSampleDocumentBitmap(doc.name, doc.category, doc.filterMode, 1)
            FileOutputStream(fallbackFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            listOf(fallbackFile.absolutePath)
        }

        val pdfFile = if (doc.pdfPath.isNotBlank()) File(doc.pdfPath) else File(docsDir, "doc_${doc.id}.pdf")
        writePagesToPdfFile(
            pagePaths = resolvedPaths,
            targetPdfFile = pdfFile,
            pageSize = doc.resolvedPageSize,
            fitMode = doc.resolvedFitMode,
            marginMode = doc.resolvedMarginMode,
            qualityDpi = doc.resolvedQualityDpi
        )

        val thumbPath = resolvedPaths.first()
        val updated = doc.copy(
            pageCount = resolvedPaths.size,
            pdfPath = pdfFile.absolutePath,
            thumbnailPath = thumbPath,
            pagePathsJoined = resolvedPaths.joinToString("|")
        )
        scanDao.updateDocument(updated)
        updated
    }

    /**
     * Rotates a single page (both original JPEG and filtered JPEG) by 90 degrees clockwise
     * and re-exports the PDF from the rotated files.
     */
    suspend fun rotateDocumentPage(
        context: Context,
        doc: DocumentRecord,
        pageIndex: Int
    ): DocumentRecord = withContext(Dispatchers.IO) {
        val pagePaths = doc.pagePaths.toMutableList()
        if (pageIndex !in pagePaths.indices) return@withContext doc
        val origPaths = doc.originalPagePaths.toMutableList()
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val ts = System.currentTimeMillis()

        // Rotate original JPEG if present
        if (pageIndex in origPaths.indices) {
            val origFile = File(origPaths[pageIndex])
            if (origFile.exists()) {
                val origBmp = BitmapFactory.decodeFile(origFile.absolutePath)
                if (origBmp != null) {
                    val rotatedOrig = rotateBitmap90(origBmp)
                    val newOrigFile = File(docsDir, "doc_${doc.id}_orig_p${pageIndex + 1}_$ts.jpg")
                    FileOutputStream(newOrigFile).use { out ->
                        rotatedOrig.compress(Bitmap.CompressFormat.JPEG, 96, out)
                    }
                    origPaths[pageIndex] = newOrigFile.absolutePath
                }
            }
        }

        // Rotate display JPEG (or regenerate from rotated original)
        val sourceFile = if (pageIndex in origPaths.indices && File(origPaths[pageIndex]).exists()) {
            File(origPaths[pageIndex])
        } else {
            File(pagePaths[pageIndex])
        }
        if (sourceFile.exists()) {
            val srcBmp = BitmapFactory.decodeFile(sourceFile.absolutePath)
            if (srcBmp != null) {
                val finalBmp = if (sourceFile.absolutePath == origPaths.getOrNull(pageIndex)) {
                    applyFilterToBitmap(srcBmp, doc.filterMode)
                } else {
                    rotateBitmap90(srcBmp)
                }
                val newPageFile = File(docsDir, "doc_${doc.id}_p${pageIndex + 1}_$ts.jpg")
                FileOutputStream(newPageFile).use { out ->
                    finalBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                pagePaths[pageIndex] = newPageFile.absolutePath
            }
        }

        updateDocumentPages(
            context = context,
            doc = doc.copy(originalPagePathsJoined = origPaths.joinToString("|")),
            newPagePaths = pagePaths
        )
    }

    /**
     * Merges sourceDoc's pages into targetDoc (up to 50 pages), regenerates the PDF, and deletes sourceDoc.
     */
    suspend fun mergeDocuments(
        context: Context,
        targetDoc: DocumentRecord,
        sourceDoc: DocumentRecord
    ): DocumentRecord = withContext(Dispatchers.IO) {
        val combinedPages = (targetDoc.pagePaths + sourceDoc.pagePaths).take(50)
        val combinedOrig = (targetDoc.originalPagePaths + sourceDoc.originalPagePaths).take(50)
        val combinedOcr = listOf(targetDoc.ocrText, sourceDoc.ocrText)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        val updatedTarget = updateDocumentPages(
            context = context,
            doc = targetDoc.copy(
                ocrText = combinedOcr,
                originalPagePathsJoined = combinedOrig.joinToString("|")
            ),
            newPagePaths = combinedPages
        )
        scanDao.deleteDocumentById(sourceDoc.id)
        updatedTarget
    }

    /**
     * Runs bundled offline ML Kit Text Recognition across all pages of the document,
     * saves the result into Room's ocrText column, and returns the extracted text.
     */
    suspend fun extractOcrTextForDocument(
        context: Context,
        doc: DocumentRecord
    ): String = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val sb = StringBuilder()

        doc.pagePaths.forEachIndexed { idx, path ->
            val file = File(path)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) {
                    val pageText = suspendCancellableCoroutine<String> { cont ->
                        val image = InputImage.fromBitmap(bmp, 0)
                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                cont.resume(visionText.text.trim())
                            }
                            .addOnFailureListener {
                                cont.resume("")
                            }
                    }
                    if (pageText.isNotBlank()) {
                        if (sb.isNotEmpty()) sb.append("\n\n")
                        if (doc.pageCount > 1) sb.append("--- Page ${idx + 1} ---\n")
                        sb.append(pageText)
                    }
                }
            }
        }

        val finalOcr = sb.toString().ifBlank {
            doc.ocrText.ifBlank {
                "Document: ${doc.name}\nCategory: ${doc.category}\nPages: ${doc.pageCount}\nCaptured offline with Dyne Document Scanner."
            }
        }
        scanDao.updateDocumentOcrText(doc.id, finalOcr)
        finalOcr
    }

    /**
     * Free tier one-page OCR preview: runs offline ML Kit Text Recognition on the first page (or specified pageIndex) only.
     */
    suspend fun extractOnePageOcrPreview(
        context: Context,
        doc: DocumentRecord,
        pageIndex: Int = 0
    ): String = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val path = doc.pagePaths.getOrNull(pageIndex) ?: doc.pagePaths.firstOrNull()
        var pageText = ""
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) {
                    pageText = suspendCancellableCoroutine { cont ->
                        val image = InputImage.fromBitmap(bmp, 0)
                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                cont.resume(visionText.text.trim())
                            }
                            .addOnFailureListener {
                                cont.resume("")
                            }
                    }
                }
            }
        }
        val finalPreview = pageText.ifBlank {
            doc.ocrText.lines().take(18).joinToString("\n").ifBlank {
                "Page ${pageIndex + 1} OCR Preview\nDocument: ${doc.name}\nCategory: ${doc.category}"
            }
        }
        if (doc.ocrText.isBlank()) {
            scanDao.updateDocumentOcrText(doc.id, finalPreview)
        }
        finalPreview
    }

    /**
     * Applies Pro Scan Filters (Magic Color, B&W Adaptive, Grayscale, Whiteboard, Shadow Removal)
     * plus brightness, contrast, and sharpen sliders to either a single page or batch-applied across all pages.
     */
    suspend fun applySmartScanFilterAndSlidersToDocument(
        context: Context,
        doc: DocumentRecord,
        filterMode: String,
        brightnessDelta: Float,
        contrastScale: Float,
        sharpenAmount: Float,
        batchApplyAllPages: Boolean,
        targetPageIndex: Int = 0
    ): DocumentRecord = withContext(Dispatchers.IO) {
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val origPaths = doc.originalPagePaths.ifEmpty { doc.pagePaths }
        val currentDisplayPaths = doc.pagePaths.toMutableList()
        val ts = System.currentTimeMillis()

        val indicesToProcess = if (batchApplyAllPages) {
            origPaths.indices.toList()
        } else {
            listOf(targetPageIndex.coerceIn(0, (origPaths.size - 1).coerceAtLeast(0)))
        }

        for (idx in indicesToProcess) {
            val origFile = File(origPaths[idx])
            if (origFile.exists()) {
                val origBmp = BitmapFactory.decodeFile(origFile.absolutePath)
                if (origBmp != null) {
                    val processed = DocumentSmartTools.applySmartFilterAndSliders(
                        source = origBmp,
                        filterLabel = filterMode,
                        brightnessDelta = brightnessDelta,
                        contrastScale = contrastScale,
                        sharpenAmount = sharpenAmount
                    )
                    val outFile = File(docsDir, "doc_${doc.id}_pro_p${idx + 1}_$ts.jpg")
                    FileOutputStream(outFile).use { out ->
                        processed.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    if (idx in currentDisplayPaths.indices) {
                        currentDisplayPaths[idx] = outFile.absolutePath
                    } else {
                        currentDisplayPaths.add(outFile.absolutePath)
                    }
                }
            }
        }

        val pdfFile = if (doc.pdfPath.isNotBlank()) File(doc.pdfPath) else File(docsDir, "doc_${doc.id}.pdf")
        writePagesToPdfFile(
            pagePaths = currentDisplayPaths,
            targetPdfFile = pdfFile,
            pageSize = doc.resolvedPageSize,
            fitMode = doc.resolvedFitMode,
            marginMode = doc.resolvedMarginMode,
            qualityDpi = doc.resolvedQualityDpi
        )

        val updated = doc.copy(
            pagePathsJoined = currentDisplayPaths.joinToString("|"),
            thumbnailPath = currentDisplayPaths.firstOrNull() ?: doc.thumbnailPath,
            filterMode = filterMode,
            brightnessDelta = brightnessDelta,
            contrastScale = contrastScale,
            sharpenAmount = sharpenAmount,
            pdfPath = pdfFile.absolutePath
        )
        scanDao.updateDocument(updated)
        updated
    }

    /**
     * Merges an arbitrary list of documents into the first document (Unlimited merge for Pro, up to 3 for Free).
     */
    suspend fun mergeMultipleDocuments(
        context: Context,
        docsToMerge: List<DocumentRecord>,
        maxPages: Int = 200
    ): DocumentRecord? = withContext(Dispatchers.IO) {
        if (docsToMerge.size < 2) return@withContext docsToMerge.firstOrNull()
        val targetDoc = docsToMerge.first()
        val otherDocs = docsToMerge.drop(1)
        val combinedPages = docsToMerge.flatMap { it.pagePaths }.take(maxPages)
        val combinedOrig = docsToMerge.flatMap { it.originalPagePaths.ifEmpty { it.pagePaths } }.take(maxPages)
        val combinedOcr = docsToMerge.map { it.ocrText }.filter { it.isNotBlank() }.joinToString("\n\n")
        val updatedTarget = updateDocumentPages(
            context = context,
            doc = targetDoc.copy(
                ocrText = combinedOcr,
                originalPagePathsJoined = combinedOrig.joinToString("|")
            ),
            newPagePaths = combinedPages
        )
        otherDocs.forEach { scanDao.deleteDocumentById(it.id) }
        updatedTarget
    }

    suspend fun updateDocumentRecord(doc: DocumentRecord) {
        scanDao.updateDocument(doc)
    }

    suspend fun saveEditedOcrText(docId: Long, newText: String) {
        scanDao.updateDocumentOcrText(docId, newText)
    }

    suspend fun renameDocument(docId: Long, newName: String) {
        if (newName.isNotBlank()) {
            scanDao.renameDocument(docId, newName.trim())
        }
    }

    suspend fun toggleDocumentFavorite(doc: DocumentRecord) {
        scanDao.setDocumentFavorite(doc.id, !doc.favorite)
    }

    suspend fun setDocumentLocked(docId: Long, locked: Boolean) {
        scanDao.setDocumentLocked(docId, locked)
    }

    suspend fun deleteDocument(id: Long) {
        scanDao.deleteDocumentById(id)
    }

    suspend fun restoreDeletedDocument(doc: DocumentRecord) {
        scanDao.insertDocument(doc)
    }

    /**
     * Shares a Document's PDF via Android Sharesheet using FileProvider.
     */
    suspend fun createSharePdfIntent(context: Context, doc: DocumentRecord): Intent? = withContext(Dispatchers.IO) {
        val pdfFile = File(doc.pdfPath)
        if (!pdfFile.exists()) {
            writePagesToPdfFile(doc.pagePaths, pdfFile)
        }
        if (!pdfFile.exists()) return@withContext null

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, doc.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Saves the document PDF to the user's phone Downloads folder using MediaStore (zero storage permission required).
     */
    suspend fun savePdfToPhoneDownloads(context: Context, doc: DocumentRecord): String? = withContext(Dispatchers.IO) {
        val pdfFile = File(doc.pdfPath)
        if (!pdfFile.exists()) {
            writePagesToPdfFile(doc.pagePaths, pdfFile)
        }
        if (!pdfFile.exists()) return@withContext null

        val cleanFileName = doc.name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
            .ifBlank { "Dyne_Document_${doc.id}" } + ".pdf"

        runCatching {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Dyne")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext null

            resolver.openOutputStream(uri)?.use { out ->
                pdfFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            "Downloads/Dyne/$cleanFileName"
        }.getOrNull()
    }

    suspend fun saveNote(
        title: String,
        content: String,
        categoryTag: String = "Measurement",
        colorHex: String = "#CCFF00",
        isPinned: Boolean = false,
        isDrawing: Boolean = false,
        drawingPngPath: String = "",
        existingId: Long = 0L,
        noteType: String = if (isDrawing) "DRAWING" else "TEXT",
        checklistJson: String = "",
        tagsCsv: String = "",
        folderName: String = "",
        isLocked: Boolean = false,
        reminderEpochMs: Long = 0L,
        attachedImagePathsJoined: String = "",
        paperStyleId: String = "PLAIN_WHITE",
        strokesJson: String = "",
        recognizedHandwritingText: String = ""
    ): NoteRecord {
        val note = NoteRecord(
            id = existingId,
            title = title.ifBlank {
                when (noteType) {
                    "DRAWING" -> "Sketch"
                    "CHECKLIST" -> "Checklist"
                    else -> "Field Note"
                }
            },
            content = content,
            categoryTag = categoryTag,
            colorHex = colorHex,
            isPinned = isPinned,
            isDrawing = isDrawing || noteType == "DRAWING",
            drawingPngPath = drawingPngPath,
            timestamp = System.currentTimeMillis(),
            noteType = noteType,
            checklistJson = checklistJson,
            tagsCsv = tagsCsv,
            folderName = folderName,
            isLocked = isLocked,
            reminderEpochMs = reminderEpochMs,
            attachedImagePathsJoined = attachedImagePathsJoined,
            paperStyleId = paperStyleId,
            strokesJson = strokesJson,
            recognizedHandwritingText = recognizedHandwritingText
        )
        val id = scanDao.insertNote(note)
        return if (existingId != 0L) note else note.copy(id = id)
    }

    suspend fun saveDrawingPngNote(
        context: Context,
        title: String,
        paperColorHex: String,
        bitmap: Bitmap,
        isPinned: Boolean = false,
        existingId: Long = 0L,
        paperStyleId: String = "PLAIN_WHITE",
        strokesJson: String = "",
        recognizedHandwritingText: String = "",
        tagsCsv: String = "",
        folderName: String = "",
        isLocked: Boolean = false
    ): NoteRecord = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val drawingsDir = File(context.filesDir, "drawings").apply { mkdirs() }
        val pngFile = File(drawingsDir, if (existingId != 0L) "sketch_${existingId}_$now.png" else "sketch_$now.png")
        FileOutputStream(pngFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val cleanTitle = title.ifBlank {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            "Sketch ${fmt.format(Date(now))}"
        }
        val summaryContent = if (recognizedHandwritingText.isNotBlank()) {
            recognizedHandwritingText
        } else {
            "Drawing Pad Sketch • ${bitmap.width}×${bitmap.height} PNG"
        }
        val note = NoteRecord(
            id = existingId,
            title = cleanTitle,
            content = summaryContent,
            categoryTag = "Drawing",
            colorHex = paperColorHex,
            isPinned = isPinned,
            isDrawing = true,
            drawingPngPath = pngFile.absolutePath,
            timestamp = now,
            noteType = "DRAWING",
            tagsCsv = tagsCsv,
            folderName = folderName,
            isLocked = isLocked,
            paperStyleId = paperStyleId,
            strokesJson = strokesJson,
            recognizedHandwritingText = recognizedHandwritingText
        )
        val id = scanDao.insertNote(note)
        if (existingId != 0L) note else note.copy(id = id)
    }

    suspend fun copyUriToNoteImages(context: Context, uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "note_images").apply { mkdirs() }
            val dest = File(dir, "note_img_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bmp = BitmapFactory.decodeStream(input) ?: return@withContext null
                FileOutputStream(dest).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
            }
            if (dest.exists() && dest.length() > 0L) dest.absolutePath else null
        }.getOrNull()
    }

    suspend fun exportNoteToPdfIntent(
        context: Context,
        note: NoteRecord,
        pageSize: PdfPageSize = PdfPageSize.A4
    ): Intent? = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanBase = note.title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
                .replace(" ", "_").ifBlank { "Dyne_Note_${note.id}" }
            val pdfFile = File(exportDir, "${cleanBase}_${pageSize.id}.pdf")

            val targetSize = if (pageSize == PdfPageSize.AUTO) PdfPageSize.A4 else pageSize
            val pageW = targetSize.widthPt.coerceAtLeast(300)
            val pageH = targetSize.heightPt.coerceAtLeast(400)

            val pdfDoc = PdfDocument()
            try {
                if (note.isDrawing && note.drawingPngPath.isNotBlank() && File(note.drawingPngPath).exists()) {
                    val bmp = BitmapFactory.decodeFile(note.drawingPngPath)
                    if (bmp != null) {
                        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
                        val page = pdfDoc.startPage(pageInfo)
                        val canvas = page.canvas
                        canvas.drawColor(Color.WHITE)
                        val margin = 24f
                        val availW = pageW - margin * 2f
                        val availH = pageH - margin * 2f
                        val scale = minOf(availW / bmp.width.toFloat(), availH / bmp.height.toFloat())
                        val drawW = bmp.width * scale
                        val drawH = bmp.height * scale
                        val left = (pageW - drawW) / 2f
                        val top = (pageH - drawH) / 2f
                        val destRect = RectF(left, top, left + drawW, top + drawH)
                        canvas.drawBitmap(bmp, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        pdfDoc.finishPage(page)
                    }
                } else {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    val margin = 36f
                    var y = margin + 20f

                    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(15, 23, 42)
                        textSize = 18f
                        isFakeBoldText = true
                    }
                    canvas.drawText(note.title.ifBlank { "Untitled Note" }, margin, y, titlePaint)
                    y += 18f

                    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(100, 116, 139)
                        textSize = 9.5f
                    }
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(note.timestamp))
                    val metaLine = buildString {
                        append(dateStr)
                        if (note.folderName.isNotBlank()) append("  •  Folder: ${note.folderName}")
                        if (note.tagsCsv.isNotBlank()) append("  •  Tags: ${note.tagsCsv}")
                    }
                    canvas.drawText(metaLine, margin, y, metaPaint)
                    y += 12f

                    val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(226, 232, 240)
                        strokeWidth = 1f
                    }
                    canvas.drawLine(margin, y, pageW - margin, y, rulePaint)
                    y += 20f

                    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(30, 41, 59)
                        textSize = 11.5f
                    }
                    val maxLineWidth = pageW - margin * 2f
                    val rawLines = note.content.split("\n")
                    for (paragraph in rawLines) {
                        val words = paragraph.split(" ")
                        var currentLine = ""
                        for (word in words) {
                            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                            if (bodyPaint.measureText(testLine) > maxLineWidth && currentLine.isNotEmpty()) {
                                if (y < pageH - margin) {
                                    canvas.drawText(currentLine, margin, y, bodyPaint)
                                    y += 16f
                                }
                                currentLine = word
                            } else {
                                currentLine = testLine
                            }
                        }
                        if (currentLine.isNotEmpty() && y < pageH - margin) {
                            canvas.drawText(currentLine, margin, y, bodyPaint)
                            y += 16f
                        } else {
                            y += 8f
                        }
                    }

                    // Attached images preview if space remains
                    for (imgPath in note.attachedImagePaths) {
                        val imgFile = File(imgPath)
                        if (imgFile.exists() && y + 90f < pageH - margin) {
                            val bmp = BitmapFactory.decodeFile(imgFile.absolutePath)
                            if (bmp != null) {
                                val availW = (pageW - margin * 2f).coerceAtMost(240f)
                                val availH = (pageH - margin - y).coerceAtMost(160f)
                                val scale = minOf(availW / bmp.width.toFloat(), availH / bmp.height.toFloat())
                                val dw = bmp.width * scale
                                val dh = bmp.height * scale
                                canvas.drawBitmap(
                                    bmp,
                                    null,
                                    RectF(margin, y + 6f, margin + dw, y + 6f + dh),
                                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                                )
                                y += dh + 16f
                            }
                        }
                    }

                    pdfDoc.finishPage(page)
                }

                FileOutputStream(pdfFile).use { out ->
                    pdfDoc.writeTo(out)
                }
            } finally {
                pdfDoc.close()
            }

            // Also save a copy to Downloads/Dyne
            runCatching {
                val resolver = context.contentResolver
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Dyne")
                }
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)?.let { dlUri ->
                    resolver.openOutputStream(dlUri)?.use { out ->
                        pdfFile.inputStream().use { it.copyTo(out) }
                    }
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${note.title} (${targetSize.label})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.getOrNull()
    }

    suspend fun exportNoteTextFileIntent(
        context: Context,
        note: NoteRecord,
        asMarkdown: Boolean
    ): Intent? = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val ext = if (asMarkdown) "md" else "txt"
            val mime = if (asMarkdown) "text/markdown" else "text/plain"
            val cleanBase = note.title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
                .replace(" ", "_").ifBlank { "Dyne_Note_${note.id}" }
            val outFile = File(exportDir, "$cleanBase.$ext")

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(note.timestamp))
            val bodyText = if (asMarkdown) {
                buildString {
                    appendLine("# ${note.title.ifBlank { "Untitled Note" }}")
                    appendLine()
                    appendLine("- **Date:** $dateStr")
                    if (note.folderName.isNotBlank()) appendLine("- **Folder:** ${note.folderName}")
                    if (note.tagsCsv.isNotBlank()) appendLine("- **Tags:** ${note.tagsCsv}")
                    appendLine()
                    appendLine("---")
                    appendLine()
                    appendLine(note.content)
                }
            } else {
                buildString {
                    appendLine(note.title.ifBlank { "Untitled Note" })
                    appendLine("Date: $dateStr")
                    if (note.folderName.isNotBlank()) appendLine("Folder: ${note.folderName}")
                    if (note.tagsCsv.isNotBlank()) appendLine("Tags: ${note.tagsCsv}")
                    appendLine("----------------------------------------")
                    appendLine(note.content)
                }
            }

            outFile.writeText(bodyText)

            // Save to Downloads/Dyne as well
            runCatching {
                val resolver = context.contentResolver
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, outFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Dyne")
                }
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)?.let { dlUri ->
                    resolver.openOutputStream(dlUri)?.use { out ->
                        outFile.inputStream().use { it.copyTo(out) }
                    }
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, note.title)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.getOrNull()
    }

    suspend fun createShareDrawingIntent(context: Context, note: NoteRecord): Intent? = withContext(Dispatchers.IO) {
        if (note.drawingPngPath.isBlank()) return@withContext null
        val pngFile = File(note.drawingPngPath)
        if (!pngFile.exists()) return@withContext null

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pngFile
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun saveDrawingToPhoneGallery(context: Context, note: NoteRecord): String? = withContext(Dispatchers.IO) {
        if (note.drawingPngPath.isBlank()) return@withContext null
        val pngFile = File(note.drawingPngPath)
        if (!pngFile.exists()) return@withContext null

        val cleanFileName = note.title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
            .ifBlank { "Dyne_Sketch_${note.id}" } + ".png"

        runCatching {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Dyne")
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext null

            resolver.openOutputStream(uri)?.use { out ->
                pngFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            "Pictures/Dyne/$cleanFileName"
        }.getOrNull()
    }

    suspend fun toggleNotePinned(note: NoteRecord) {
        scanDao.setNotePinned(note.id, !note.isPinned)
    }

    suspend fun toggleNoteFavorite(note: NoteRecord) {
        scanDao.setNoteFavorite(note.id, !note.isFavorite)
    }

    suspend fun setNoteLocked(note: NoteRecord, isLocked: Boolean) {
        scanDao.setNoteLocked(note.id, isLocked)
    }

    suspend fun deleteNote(id: Long) {
        scanDao.deleteNoteById(id)
    }

    suspend fun setAllDocumentsLocked(locked: Boolean) {
        scanDao.setAllDocumentsLocked(locked)
    }

    suspend fun setAllNotesLocked(locked: Boolean) {
        scanDao.setAllNotesLocked(locked)
    }

    data class StorageUsageBreakdown(
        val documentsBytes: Long = 0L,
        val notesAndSketchesBytes: Long = 0L,
        val scansAndDatabaseBytes: Long = 0L,
        val settingsAndPrefsBytes: Long = 0L,
        val temporaryCacheBytes: Long = 0L
    ) {
        val totalBytes: Long
            get() = documentsBytes + notesAndSketchesBytes + scansAndDatabaseBytes + settingsAndPrefsBytes + temporaryCacheBytes
    }

    suspend fun computeStorageBreakdown(context: Context): StorageUsageBreakdown = withContext(Dispatchers.IO) {
        fun dirSize(dir: File): Long {
            if (!dir.exists()) return 0L
            if (dir.isFile) return dir.length()
            return dir.listFiles()?.sumOf { dirSize(it) } ?: 0L
        }
        val docsBytes = dirSize(File(context.filesDir, "documents"))
        val notesBytes = dirSize(File(context.filesDir, "drawings")) + dirSize(File(context.filesDir, "note_images"))
        val dbMain = context.getDatabasePath("dyne_local_vault.db")
        val dbWal = File(dbMain.parentFile, "dyne_local_vault.db-wal")
        val dbShm = File(dbMain.parentFile, "dyne_local_vault.db-shm")
        val dbBytes = dirSize(dbMain) + dirSize(dbWal) + dirSize(dbShm)
        val datastoreBytes = dirSize(File(context.filesDir, "datastore")) +
            dirSize(File(context.filesDir, "frankfurter_rates_cache.json")) +
            dirSize(File(context.filesDir, "unit_converter_prefs.json")) +
            dirSize(File(context.filesDir, "world_clocks_prefs.json"))
        val cacheBytes = dirSize(context.cacheDir)
        StorageUsageBreakdown(
            documentsBytes = docsBytes,
            notesAndSketchesBytes = notesBytes,
            scansAndDatabaseBytes = dbBytes,
            settingsAndPrefsBytes = datastoreBytes,
            temporaryCacheBytes = cacheBytes
        )
    }

    suspend fun exportBackupZip(
        context: Context,
        targetUri: Uri,
        preferencesRepository: PreferencesRepository,
        isFullBackup: Boolean,
        appVersion: String = "1.0.0",
        onProgress: (Float, String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(0.05f, "Preparing backup manifest...")
            val manifest = org.json.JSONObject().apply {
                put("schemaVersion", BACKUP_SCHEMA_VERSION)
                put("appVersion", appVersion)
                put("backupType", if (isFullBackup) "FULL" else "SETTINGS_ONLY")
                put("createdAtEpochMs", System.currentTimeMillis())
            }

            onProgress(0.15f, "Serializing preferences...")
            val settingsJson = preferencesRepository.exportSettingsToJson(includeCoins = isFullBackup)

            val scans = if (isFullBackup) scanDao.getAllScansSnapshot() else emptyList()
            val docs = if (isFullBackup) scanDao.getAllDocumentsSnapshot() else emptyList()
            val notes = if (isFullBackup) scanDao.getAllNotesSnapshot() else emptyList()
            val ledger = if (isFullBackup) scanDao.getAllCoinLedgerSnapshot() else emptyList()

            val outStream = context.contentResolver.openOutputStream(targetUri)
                ?: error("Unable to open selected destination file")

            java.util.zip.ZipOutputStream(outStream.buffered()).use { zos ->
                fun writeTextEntry(entryName: String, content: String) {
                    val entry = java.util.zip.ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                fun writeFileIfExists(filePath: String, zipSubDir: String): String {
                    if (filePath.isBlank()) return ""
                    val file = File(filePath)
                    if (!file.exists() || !file.isFile) return ""
                    val entryPath = "$zipSubDir/${file.name}"
                    runCatching {
                        val entry = java.util.zip.ZipEntry(entryPath)
                        zos.putNextEntry(entry)
                        file.inputStream().buffered().use { input ->
                            input.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                    return entryPath
                }

                writeTextEntry("manifest.json", manifest.toString(2))
                writeTextEntry("settings.json", settingsJson.toString(2))

                if (isFullBackup) {
                    onProgress(0.30f, "Exporting QR & barcode scans (${scans.size})...")
                    val scansArr = org.json.JSONArray()
                    scans.forEach { s ->
                        scansArr.put(
                            org.json.JSONObject().apply {
                                put("rawContent", s.rawContent)
                                put("displayTitle", s.displayTitle)
                                put("formatName", s.formatName)
                                put("is2dMatrix", s.is2dMatrix)
                                put("contentType", s.contentType)
                                put("isFavorite", s.isFavorite)
                                put("timestamp", s.timestamp)
                            }
                        )
                    }
                    writeTextEntry("scans.json", scansArr.toString(2))

                    onProgress(0.45f, "Exporting coin ledger (${ledger.size})...")
                    val ledgerArr = org.json.JSONArray()
                    ledger.forEach { c ->
                        ledgerArr.put(
                            org.json.JSONObject().apply {
                                put("delta", c.delta)
                                put("balanceAfter", c.balanceAfter)
                                put("reason", c.reason)
                                put("timestamp", c.timestamp)
                            }
                        )
                    }
                    writeTextEntry("coin_ledger.json", ledgerArr.toString(2))

                    val docsArr = org.json.JSONArray()
                    docs.forEachIndexed { idx, d ->
                        val frac = 0.50f + 0.25f * ((idx + 1).toFloat() / docs.size.coerceAtLeast(1))
                        onProgress(frac, "Archiving document ${idx + 1}/${docs.size}: ${d.name}...")
                        val pdfRel = writeFileIfExists(d.pdfPath, "files/documents")
                        val thumbRel = writeFileIfExists(d.thumbnailPath, "files/documents")
                        val pageRels = d.pagePaths.map { writeFileIfExists(it, "files/documents") }.filter { it.isNotEmpty() }
                        val origRels = d.originalPagePaths.map { writeFileIfExists(it, "files/documents") }.filter { it.isNotEmpty() }
                        docsArr.put(
                            org.json.JSONObject().apply {
                                put("name", d.name)
                                put("category", d.category)
                                put("createdAt", d.createdAt)
                                put("pageCount", d.pageCount)
                                put("pdfRel", pdfRel)
                                put("thumbRel", thumbRel)
                                put("pageRels", pageRels.joinToString("|"))
                                put("origRels", origRels.joinToString("|"))
                                put("ocrText", d.ocrText)
                                put("favorite", d.favorite)
                                put("locked", d.locked)
                                put("filterMode", d.filterMode)
                                put("pageSizeId", d.pageSizeId)
                                put("fitModeId", d.fitModeId)
                                put("marginModeId", d.marginModeId)
                                put("qualityDpiId", d.qualityDpiId)
                                put("folderName", d.folderName)
                                put("tagsCsv", d.tagsCsv)
                                put("expiryEpochMs", d.expiryEpochMs)
                                put("merchantName", d.merchantName)
                                put("receiptDateIso", d.receiptDateIso)
                                put("receiptTotalAmount", d.receiptTotalAmount)
                                put("expenseCategory", d.expenseCategory)
                            }
                        )
                    }
                    writeTextEntry("documents.json", docsArr.toString(2))

                    val notesArr = org.json.JSONArray()
                    notes.forEachIndexed { idx, n ->
                        val frac = 0.75f + 0.20f * ((idx + 1).toFloat() / notes.size.coerceAtLeast(1))
                        onProgress(frac, "Archiving note/sketch ${idx + 1}/${notes.size}: ${n.title}...")
                        val drawingRel = writeFileIfExists(n.drawingPngPath, "files/drawings")
                        val attachedRels = n.attachedImagePaths.map { writeFileIfExists(it, "files/note_images") }.filter { it.isNotEmpty() }
                        notesArr.put(
                            org.json.JSONObject().apply {
                                put("title", n.title)
                                put("content", n.content)
                                put("categoryTag", n.categoryTag)
                                put("colorHex", n.colorHex)
                                put("isPinned", n.isPinned)
                                put("isDrawing", n.isDrawing)
                                put("drawingRel", drawingRel)
                                put("isFavorite", n.isFavorite)
                                put("timestamp", n.timestamp)
                                put("noteType", n.noteType)
                                put("checklistJson", n.checklistJson)
                                put("tagsCsv", n.tagsCsv)
                                put("folderName", n.folderName)
                                put("isLocked", n.isLocked)
                                put("reminderEpochMs", n.reminderEpochMs)
                                put("attachedRels", attachedRels.joinToString("|"))
                                put("paperStyleId", n.paperStyleId)
                                put("strokesJson", n.strokesJson)
                                put("recognizedHandwritingText", n.recognizedHandwritingText)
                            }
                        )
                    }
                    writeTextEntry("notes.json", notesArr.toString(2))
                }

                onProgress(1.0f, "Backup complete")
            }

            if (isFullBackup) {
                "Exported full ZIP backup (${docs.size} docs, ${notes.size} notes/sketches, ${scans.size} scans, settings & coins)"
            } else {
                "Exported settings backup to ZIP"
            }
        }
    }

    suspend fun restoreBackupZip(
        context: Context,
        sourceUri: Uri,
        preferencesRepository: PreferencesRepository,
        allowFullRestore: Boolean,
        onProgress: (Float, String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(0.08f, "Reading backup archive & checking version...")
            val inStream = context.contentResolver.openInputStream(sourceUri)
                ?: error("Unable to open selected backup file")

            var manifestJsonStr: String? = null
            var settingsJsonStr: String? = null
            var scansJsonStr: String? = null
            var docsJsonStr: String? = null
            var notesJsonStr: String? = null
            var ledgerJsonStr: String? = null
            val extractedFiles = mutableMapOf<String, String>()

            java.util.zip.ZipInputStream(inStream.buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory) {
                        when {
                            name == "manifest.json" -> {
                                manifestJsonStr = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            name == "settings.json" -> {
                                settingsJsonStr = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            name == "scans.json" -> {
                                scansJsonStr = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            name == "documents.json" -> {
                                docsJsonStr = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            name == "notes.json" -> {
                                notesJsonStr = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            name == "coin_ledger.json" -> {
                                ledgerJsonStr = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            allowFullRestore && (name.startsWith("files/documents/") || name.startsWith("files/drawings/") || name.startsWith("files/note_images/")) -> {
                                val subFolder = name.removePrefix("files/").substringBefore("/")
                                val fileName = File(name).name
                                if (fileName.isNotBlank()) {
                                    val destDir = File(context.filesDir, subFolder).apply { mkdirs() }
                                    val destFile = File(destDir, fileName)
                                    FileOutputStream(destFile).use { out ->
                                        zis.copyTo(out)
                                    }
                                    extractedFiles[name] = destFile.absolutePath
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Version check on manifest.json
            val manifestObj = manifestJsonStr?.let { org.json.JSONObject(it) }
                ?: error("Invalid Dyne backup archive: missing manifest.json")
            val schemaVersion = manifestObj.optInt("schemaVersion", -1)
            val backupAppVersion = manifestObj.optString("appVersion", "unknown")
            if (schemaVersion < 1 || schemaVersion > BACKUP_SCHEMA_VERSION) {
                error("Incompatible backup schema v$schemaVersion (from app v$backupAppVersion). Supported schema: v1..v$BACKUP_SCHEMA_VERSION.")
            }

            onProgress(0.35f, "Verified backup v$schemaVersion (App $backupAppVersion). Restoring settings...")
            val settingsObj = settingsJsonStr?.let { org.json.JSONObject(it) }
                ?: error("Backup archive is missing settings.json")
            preferencesRepository.restoreSettingsFromJson(settingsObj, includeCoins = allowFullRestore)

            var restoredDocs = 0
            var restoredNotes = 0
            var restoredScans = 0

            if (allowFullRestore) {
                scansJsonStr?.let { raw ->
                    onProgress(0.55f, "Restoring scans...")
                    val arr = org.json.JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        scanDao.insertScan(
                            ScanRecord(
                                rawContent = o.optString("rawContent", ""),
                                displayTitle = o.optString("displayTitle", ""),
                                formatName = o.optString("formatName", "QR_CODE"),
                                is2dMatrix = o.optBoolean("is2dMatrix", true),
                                contentType = o.optString("contentType", "TEXT"),
                                isFavorite = o.optBoolean("isFavorite", false),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                        restoredScans++
                    }
                }

                ledgerJsonStr?.let { raw ->
                    val arr = org.json.JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        scanDao.insertCoinLedger(
                            CoinLedgerRecord(
                                delta = o.optInt("delta", 0),
                                balanceAfter = o.optInt("balanceAfter", 0),
                                reason = o.optString("reason", "Restored"),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }

                docsJsonStr?.let { raw ->
                    onProgress(0.75f, "Restoring documents...")
                    val arr = org.json.JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val pdfRel = o.optString("pdfRel", "")
                        val thumbRel = o.optString("thumbRel", "")
                        val pageRels = o.optString("pageRels", "").split("|").mapNotNull { extractedFiles[it] }
                        val origRels = o.optString("origRels", "").split("|").mapNotNull { extractedFiles[it] }
                        scanDao.insertDocument(
                            DocumentRecord(
                                name = o.optString("name", "Restored Document"),
                                category = o.optString("category", "Other"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                                pageCount = o.optInt("pageCount", pageRels.size.coerceAtLeast(1)),
                                pdfPath = extractedFiles[pdfRel] ?: "",
                                thumbnailPath = extractedFiles[thumbRel] ?: pageRels.firstOrNull().orEmpty(),
                                pagePathsJoined = pageRels.joinToString("|"),
                                originalPagePathsJoined = origRels.joinToString("|"),
                                ocrText = o.optString("ocrText", ""),
                                favorite = o.optBoolean("favorite", false),
                                locked = o.optBoolean("locked", false),
                                filterMode = o.optString("filterMode", "Original Color"),
                                pageSizeId = o.optString("pageSizeId", ""),
                                fitModeId = o.optString("fitModeId", "FIT"),
                                marginModeId = o.optString("marginModeId", "SMALL_10MM"),
                                qualityDpiId = o.optString("qualityDpiId", "HIGH_200"),
                                folderName = o.optString("folderName", ""),
                                tagsCsv = o.optString("tagsCsv", ""),
                                expiryEpochMs = o.optLong("expiryEpochMs", 0L),
                                merchantName = o.optString("merchantName", ""),
                                receiptDateIso = o.optString("receiptDateIso", ""),
                                receiptTotalAmount = o.optDouble("receiptTotalAmount", 0.0),
                                expenseCategory = o.optString("expenseCategory", "")
                            )
                        )
                        restoredDocs++
                    }
                }

                notesJsonStr?.let { raw ->
                    onProgress(0.90f, "Restoring notes & sketches...")
                    val arr = org.json.JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val drawingRel = o.optString("drawingRel", "")
                        val attachedRels = o.optString("attachedRels", "").split("|").mapNotNull { extractedFiles[it] }
                        scanDao.insertNote(
                            NoteRecord(
                                title = o.optString("title", "Restored Note"),
                                content = o.optString("content", ""),
                                categoryTag = o.optString("categoryTag", "Note"),
                                colorHex = o.optString("colorHex", "#C6F27A"),
                                isPinned = o.optBoolean("isPinned", false),
                                isDrawing = o.optBoolean("isDrawing", false),
                                drawingPngPath = extractedFiles[drawingRel] ?: "",
                                isFavorite = o.optBoolean("isFavorite", false),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                                noteType = o.optString("noteType", "TEXT"),
                                checklistJson = o.optString("checklistJson", ""),
                                tagsCsv = o.optString("tagsCsv", ""),
                                folderName = o.optString("folderName", ""),
                                isLocked = o.optBoolean("isLocked", false),
                                reminderEpochMs = o.optLong("reminderEpochMs", 0L),
                                attachedImagePathsJoined = attachedRels.joinToString("|"),
                                paperStyleId = o.optString("paperStyleId", "PLAIN_WHITE"),
                                strokesJson = o.optString("strokesJson", ""),
                                recognizedHandwritingText = o.optString("recognizedHandwritingText", "")
                            )
                        )
                        restoredNotes++
                    }
                }
            }

            onProgress(1.0f, "Restore complete")
            if (allowFullRestore && (restoredDocs > 0 || restoredNotes > 0 || restoredScans > 0)) {
                "Restored settings, $restoredDocs docs, $restoredNotes notes/sketches & $restoredScans scans (schema v$schemaVersion)"
            } else {
                "Restored settings from backup (schema v$schemaVersion)"
            }
        }
    }

    suspend fun clearAllLocalVaultData(context: Context) = withContext(Dispatchers.IO) {
        scanDao.deleteAllScans()
        scanDao.deleteAllDocuments()
        scanDao.deleteAllNotes()
        scanDao.deleteAllCoinLedger()
        scanDao.deleteAllMediaRecentFiles()
        runCatching {
            File(context.filesDir, "documents").deleteRecursively()
            File(context.filesDir, "drawings").deleteRecursively()
            File(context.filesDir, "note_images").deleteRecursively()
            File(context.filesDir, "frankfurter_rates_cache.json").delete()
            File(context.filesDir, "unit_converter_prefs.json").delete()
            File(context.filesDir, "world_clocks_prefs.json").delete()
            File(context.cacheDir, "media_studio").deleteRecursively()
            File(context.cacheDir, "exports").deleteRecursively()
        }
    }

    suspend fun computeTemporaryCacheSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        fun dirSize(dir: File): Long {
            if (!dir.exists()) return 0L
            if (dir.isFile) return dir.length()
            return dir.listFiles()?.sumOf { dirSize(it) } ?: 0L
        }
        dirSize(context.cacheDir)
    }

    suspend fun cleanTemporaryCache(context: Context): Long = withContext(Dispatchers.IO) {
        val before = computeTemporaryCacheSizeBytes(context)
        runCatching {
            context.cacheDir.listFiles()?.forEach { child ->
                child.deleteRecursively()
            }
        }
        val after = computeTemporaryCacheSizeBytes(context)
        (before - after).coerceAtLeast(0L)
    }

    suspend fun exportToCsvShareIntent(context: Context): Intent? = withContext(Dispatchers.IO) {
        val records = scanDao.getAllScansSnapshot()
        if (records.isEmpty()) return@withContext null

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, "dyne_scans_export.csv")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())

        val sb = StringBuilder()
        sb.appendLine("ID,Timestamp,Format,Category,ContentType,Favorite,Title,RawContent")
        for (r in records) {
            val dateStr = formatter.format(Instant.ofEpochMilli(r.timestamp))
            val category = if (r.is2dMatrix) "QR" else "Barcode"
            sb.append(r.id).append(',')
                .append(escapeCsv(dateStr)).append(',')
                .append(escapeCsv(r.formatName)).append(',')
                .append(escapeCsv(category)).append(',')
                .append(escapeCsv(r.contentType)).append(',')
                .append(r.isFavorite).append(',')
                .append(escapeCsv(r.displayTitle)).append(',')
                .append(escapeCsv(r.rawContent))
                .appendLine()
        }
        csvFile.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dyne Scan History Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun writePagesToPdfFile(
        pagePaths: List<String>,
        targetPdfFile: File,
        pageSize: PdfPageSize = PdfPageSize.defaultForLocale(),
        fitMode: PdfFitMode = PdfFitMode.FIT,
        marginMode: PdfMarginMode = PdfMarginMode.SMALL_10MM,
        qualityDpi: PdfQualityDpi = PdfQualityDpi.HIGH_200
    ) {
        val pdfDocument = PdfDocument()
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        try {
            pagePaths.forEachIndexed { idx, path ->
                val rawBmp = BitmapFactory.decodeFile(path)
                val srcW = rawBmp?.width ?: 595
                val srcH = rawBmp?.height ?: 842
                val (pageWidthPt, pageHeightPt) = pageSize.resolvePageDimensionsPt(srcW, srcH)

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, idx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                if (rawBmp != null) {
                    val bmpToDraw = downsampleBitmapForDpi(rawBmp, pageWidthPt, pageHeightPt, qualityDpi)
                    val marginPt = if (fitMode == PdfFitMode.FILL) 0f else marginMode.marginPt.toFloat()
                    val availW = (pageWidthPt - marginPt * 2f).coerceAtLeast(36f)
                    val availH = (pageHeightPt - marginPt * 2f).coerceAtLeast(36f)

                    val bmpW = bmpToDraw.width.toFloat().coerceAtLeast(1f)
                    val bmpH = bmpToDraw.height.toFloat().coerceAtLeast(1f)
                    val scale = if (fitMode == PdfFitMode.FILL) {
                        max(availW / bmpW, availH / bmpH)
                    } else {
                        min(availW / bmpW, availH / bmpH)
                    }

                    val drawW = bmpW * scale
                    val drawH = bmpH * scale
                    val centerX = pageWidthPt / 2f
                    val centerY = pageHeightPt / 2f
                    val left = centerX - drawW / 2f
                    val top = centerY - drawH / 2f
                    val destRect = RectF(left, top, left + drawW, top + drawH)

                    if (fitMode == PdfFitMode.FIT && marginPt > 0f) {
                        canvas.save()
                        canvas.clipRect(marginPt, marginPt, pageWidthPt - marginPt, pageHeightPt - marginPt)
                        canvas.drawBitmap(bmpToDraw, null, destRect, bitmapPaint)
                        canvas.restore()
                    } else {
                        canvas.drawBitmap(bmpToDraw, null, destRect, bitmapPaint)
                    }

                    var bmpToRecycle: Bitmap? = null
                    if (bmpToDraw !== rawBmp) {
                        bmpToRecycle = bmpToDraw
                    }
                    pdfDocument.finishPage(page)
                    bmpToRecycle?.recycle()
                    rawBmp.recycle()
                } else {
                    pdfDocument.finishPage(page)
                }
            }
            targetPdfFile.parentFile?.mkdirs()
            FileOutputStream(targetPdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    companion object {
        const val BACKUP_SCHEMA_VERSION = 1

        fun defaultDocumentName(timestampMs: Long = System.currentTimeMillis()): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            return "Scan ${fmt.format(Date(timestampMs))}"
        }

        /**
         * Decodes a JPEG/image Uri at full resolution while applying EXIF rotation and flip metadata.
         */
        fun decodeUriWithExifRotation(context: Context, uri: Uri): Bitmap? {
            return runCatching {
                val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL

                val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: return@runCatching null

                applyExifOrientation(raw, orientation)
            }.getOrNull()
        }

        fun applyExifOrientation(source: Bitmap, orientation: Int): Bitmap {
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1f, 1f)
                }
                else -> return source
            }
            return runCatching {
                Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            }.getOrDefault(source)
        }

        fun downsampleBitmapForDpi(
            source: Bitmap,
            pageWidthPt: Int,
            pageHeightPt: Int,
            qualityDpi: PdfQualityDpi
        ): Bitmap {
            if (qualityDpi.dpi <= 0) return source
            val maxW = ((pageWidthPt / 72f) * qualityDpi.dpi).roundToInt().coerceAtLeast(180)
            val maxH = ((pageHeightPt / 72f) * qualityDpi.dpi).roundToInt().coerceAtLeast(180)
            if (source.width <= maxW && source.height <= maxH) return source
            val ratio = min(maxW.toFloat() / source.width.toFloat(), maxH.toFloat() / source.height.toFloat())
            val targetW = (source.width * ratio).roundToInt().coerceAtLeast(64)
            val targetH = (source.height * ratio).roundToInt().coerceAtLeast(64)
            return runCatching {
                Bitmap.createScaledBitmap(source, targetW, targetH, true)
            }.getOrDefault(source)
        }

        fun estimatePdfSizeBytes(
            bitmaps: List<Bitmap>,
            pageSize: PdfPageSize,
            qualityDpi: PdfQualityDpi
        ): Long {
            if (bitmaps.isEmpty()) return 0L
            var totalBytes = 4096L // PDF header overhead
            for (bmp in bitmaps) {
                val (wPt, hPt) = pageSize.resolvePageDimensionsPt(bmp.width, bmp.height)
                val targetW: Int
                val targetH: Int
                if (qualityDpi.dpi <= 0) {
                    targetW = bmp.width
                    targetH = bmp.height
                } else {
                    val maxW = ((wPt / 72f) * qualityDpi.dpi).roundToInt()
                    val maxH = ((hPt / 72f) * qualityDpi.dpi).roundToInt()
                    val ratio = min(1f, min(maxW.toFloat() / bmp.width.coerceAtLeast(1), maxH.toFloat() / bmp.height.coerceAtLeast(1)))
                    targetW = (bmp.width * ratio).roundToInt().coerceAtLeast(64)
                    targetH = (bmp.height * ratio).roundToInt().coerceAtLeast(64)
                }
                val pixels = targetW.toLong() * targetH.toLong()
                // Typical compressed stream in Android PdfDocument is ~0.32 bytes/pixel
                totalBytes += (pixels * 32L) / 100L + 2048L
            }
            return totalBytes
        }

        fun estimatePdfSizeBytesFromFiles(
            paths: List<String>,
            pageSize: PdfPageSize,
            qualityDpi: PdfQualityDpi
        ): Long {
            if (paths.isEmpty()) return 0L
            var totalBytes = 4096L
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            for (path in paths) {
                val file = File(path)
                if (!file.exists()) continue
                BitmapFactory.decodeFile(path, opts)
                val srcW = opts.outWidth.coerceAtLeast(595)
                val srcH = opts.outHeight.coerceAtLeast(842)
                val (wPt, hPt) = pageSize.resolvePageDimensionsPt(srcW, srcH)
                val targetW: Int
                val targetH: Int
                if (qualityDpi.dpi <= 0) {
                    targetW = srcW
                    targetH = srcH
                } else {
                    val maxW = ((wPt / 72f) * qualityDpi.dpi).roundToInt()
                    val maxH = ((hPt / 72f) * qualityDpi.dpi).roundToInt()
                    val ratio = min(1f, min(maxW.toFloat() / srcW, maxH.toFloat() / srcH))
                    targetW = (srcW * ratio).roundToInt().coerceAtLeast(64)
                    targetH = (srcH * ratio).roundToInt().coerceAtLeast(64)
                }
                val pixels = targetW.toLong() * targetH.toLong()
                totalBytes += (pixels * 32L) / 100L + 2048L
            }
            return totalBytes
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0L) return "0 KB"
            val kb = bytes / 1024.0
            return if (kb < 1024.0) {
                String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
            } else {
                val mb = kb / 1024.0
                String.format(Locale.US, "%.1f MB", mb)
            }
        }

        /**
         * Rotates a bitmap 90 degrees clockwise.
         */
        fun rotateBitmap90(source: Bitmap): Bitmap {
            val matrix = Matrix().apply { postRotate(90f) }
            return runCatching {
                Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            }.getOrDefault(source)
        }

        /**
         * Applies crop & filter enhancement (Original Color, Magic Color, Crisp B&W, Grayscale) to a captured page bitmap.
         * Default is always Original Color — never binarizes by default.
         */
        fun applyFilterToBitmap(
            source: Bitmap,
            filterMode: String,
            cropCornersNormalized: List<PointF>? = null,
            brightnessDelta: Float = 0f,
            contrastScale: Float = 1f,
            sharpenAmount: Float = 0f
        ): Bitmap {
            val cropped = if (cropCornersNormalized != null && cropCornersNormalized.size == 4) {
                val minX = cropCornersNormalized.minOf { it.x }.coerceIn(0f, 0.85f)
                val maxX = cropCornersNormalized.maxOf { it.x }.coerceIn(minX + 0.1f, 1f)
                val minY = cropCornersNormalized.minOf { it.y }.coerceIn(0f, 0.85f)
                val maxY = cropCornersNormalized.maxOf { it.y }.coerceIn(minY + 0.1f, 1f)
                val x = (minX * source.width).toInt().coerceIn(0, source.width - 10)
                val y = (minY * source.height).toInt().coerceIn(0, source.height - 10)
                val w = ((maxX - minX) * source.width).toInt().coerceIn(10, source.width - x)
                val h = ((maxY - minY) * source.height).toInt().coerceIn(10, source.height - y)
                runCatching { Bitmap.createBitmap(source, x, y, w, h) }.getOrDefault(source)
            } else {
                source
            }

            return DocumentSmartTools.applySmartFilterAndSliders(
                source = cropped,
                filterLabel = filterMode,
                brightnessDelta = brightnessDelta,
                contrastScale = contrastScale,
                sharpenAmount = sharpenAmount
            )
        }

        /**
         * Generates a crisp high-resolution synthetic document page bitmap with readable text so
         * ML Kit OCR and PDF viewers work seamlessly even on emulators without a real paper sheet in front of the camera.
         */
        fun generateSampleDocumentBitmap(
            title: String,
            category: String,
            filterMode: String,
            pageNumber: Int
        ): Bitmap {
            val width = 800
            val height = 1120
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 30, 35)
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawRect(36f, 36f, width - 36f, height - 36f, borderPaint)

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 36f
                isFakeBoldText = true
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(45, 45, 50)
                textSize = 26f
            }

            canvas.drawText("DYNE DOCUMENT VAULT", 72f, 110f, headerPaint)
            canvas.drawText("Title: $title", 72f, 175f, bodyPaint)
            canvas.drawText("Category: $category   •   Page $pageNumber", 72f, 225f, bodyPaint)
            canvas.drawText("Filter: $filterMode", 72f, 275f, bodyPaint)

            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(180, 180, 190)
                strokeWidth = 2f
            }
            canvas.drawLine(72f, 315f, width - 72f, 315f, linePaint)

            val sampleLines = listOf(
                "OFFLINE OPTICAL CHARACTER RECOGNITION",
                "Dyne Precision Instruments & Document Vault",
                "Item 01 ............ Precision Calibration ...... OK",
                "Item 02 ............ Perspective Crop ........... OK",
                "Item 03 ............ Local App-Private Vault .... OK",
                "Notes: Stored 100% locally on device in Room & PDF."
            )
            sampleLines.forEachIndexed { idx, line ->
                canvas.drawText(line, 72f, 385f + idx * 62f, bodyPaint)
            }

            return applyFilterToBitmap(bmp, filterMode)
        }

        fun detectContentType(raw: String, is2dMatrix: Boolean): ScanContentType {
            val trimmed = raw.trim()
            val lower = trimmed.lowercase()
            return when {
                lower.startsWith("http://") || lower.startsWith("https://") ||
                    lower.matches(Regex("^(www\\.)?[a-z0-9-]+\\.[a-z]{2,}(/.*)?$")) -> ScanContentType.URL
                lower.startsWith("wifi:") -> ScanContentType.WIFI
                lower.startsWith("tel:") || trimmed.matches(Regex("^\\+?[0-9()\\-\\s]{7,18}$")) && is2dMatrix -> ScanContentType.PHONE
                lower.startsWith("mailto:") || lower.startsWith("matmsg:") ||
                    trimmed.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) -> ScanContentType.EMAIL
                lower.startsWith("smsto:") || lower.startsWith("sms:") -> ScanContentType.SMS
                lower.startsWith("begin:vcard") || lower.startsWith("mecard:") -> ScanContentType.CONTACT
                lower.startsWith("geo:") || trimmed.matches(Regex("^-?\\d{1,2}\\.\\d+,\\s*-?\\d{1,3}\\.\\d+$")) -> ScanContentType.LOCATION
                !is2dMatrix && trimmed.all { it.isDigit() } && trimmed.length in 8..14 -> ScanContentType.PRODUCT
                else -> ScanContentType.TEXT
            }
        }

        fun extractDisplaySummary(raw: String, type: ScanContentType): String {
            val trimmed = raw.trim()
            return when (type) {
                ScanContentType.WIFI -> {
                    val ssid = Regex("S:([^;]+);").find(trimmed)?.groupValues?.getOrNull(1)
                    if (!ssid.isNullOrBlank()) "Wi-Fi: $ssid" else "Wi-Fi Network"
                }
                ScanContentType.CONTACT -> {
                    val fn = Regex("FN:(.+)").find(trimmed)?.groupValues?.getOrNull(1)?.trim()
                        ?: Regex("N:([^;]+)").find(trimmed)?.groupValues?.getOrNull(1)?.trim()
                    if (!fn.isNullOrBlank()) "Contact: $fn" else "Contact Card"
                }
                ScanContentType.EMAIL -> trimmed.removePrefix("mailto:").removePrefix("MAILTO:").substringBefore("?")
                ScanContentType.PHONE -> trimmed.removePrefix("tel:").removePrefix("TEL:")
                ScanContentType.SMS -> trimmed.removePrefix("smsto:").removePrefix("SMSTO:").removePrefix("sms:").substringBefore(":")
                ScanContentType.LOCATION -> trimmed.removePrefix("geo:").removePrefix("GEO:")
                else -> trimmed.lines().firstOrNull()?.take(60) ?: trimmed.take(60)
            }
        }
    }
}
