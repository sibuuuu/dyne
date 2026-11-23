package com.example.ui.scan

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DocumentRecord
import com.example.data.MediaRecentFileRecord
import com.example.data.NoteRecord
import com.example.data.PdfFitMode
import com.example.data.PdfMarginMode
import com.example.data.PdfPageSize
import com.example.data.PdfQualityDpi
import com.example.data.ScanRecord
import com.example.data.ScanRepository
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ScanFilterCategory(val label: String) {
    ALL("All"),
    QR_ONLY("QR"),
    BARCODE_ONLY("Barcode"),
    FAVORITES("Favorites")
}

enum class DocumentCategoryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    ID("ID"),
    RECEIPT("Receipt"),
    BILL("Bill"),
    NOTES("Notes"),
    OTHER("Other")
}

enum class DocumentSortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    NAME_AZ("Name A-Z"),
    PAGES("Pages")
}

class ScanViewModel(
    val scanRepository: ScanRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ScanFilterCategory.ALL)
    val selectedFilter: StateFlow<ScanFilterCategory> = _selectedFilter.asStateFlow()

    private val _selectedDocCategory = MutableStateFlow(DocumentCategoryFilter.ALL)
    val selectedDocCategory: StateFlow<DocumentCategoryFilter> = _selectedDocCategory.asStateFlow()

    private val _selectedDocSort = MutableStateFlow(DocumentSortOrder.NEWEST)
    val selectedDocSort: StateFlow<DocumentSortOrder> = _selectedDocSort.asStateFlow()

    private val _activeResultRecord = MutableStateFlow<ScanRecord?>(null)
    val activeResultRecord: StateFlow<ScanRecord?> = _activeResultRecord.asStateFlow()

    private val _activeViewerDocId = MutableStateFlow<Long?>(null)
    val activeViewerDocument: StateFlow<DocumentRecord?> = combine(
        scanRepository.allDocuments,
        _activeViewerDocId
    ) { docs, id ->
        if (id == null) null else docs.firstOrNull { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _recentlyDeletedDocument = MutableStateFlow<DocumentRecord?>(null)
    val recentlyDeletedDocument: StateFlow<DocumentRecord?> = _recentlyDeletedDocument.asStateFlow()

    private val _ocrSheetDoc = MutableStateFlow<DocumentRecord?>(null)
    val ocrSheetDoc: StateFlow<DocumentRecord?> = _ocrSheetDoc.asStateFlow()

    private val _ocrTextDraft = MutableStateFlow("")
    val ocrTextDraft: StateFlow<String> = _ocrTextDraft.asStateFlow()

    private val _isExtractingOcr = MutableStateFlow(false)
    val isExtractingOcr: StateFlow<Boolean> = _isExtractingOcr.asStateFlow()

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    val allDocuments: StateFlow<List<DocumentRecord>> = scanRepository.allDocuments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allNotes: StateFlow<List<NoteRecord>> = scanRepository.allNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allMediaRecentFiles: StateFlow<List<MediaRecentFileRecord>> = scanRepository.allMediaRecentFiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun recordMediaRecentFile(
        fileName: String,
        toolId: String,
        toolName: String,
        mimeType: String,
        outputUriString: String = "",
        localCachePath: String = "",
        beforeSizeBytes: Long = 0L,
        afterSizeBytes: Long = 0L,
        summaryLabel: String = ""
    ) {
        viewModelScope.launch {
            scanRepository.recordMediaRecentFile(
                fileName = fileName,
                toolId = toolId,
                toolName = toolName,
                mimeType = mimeType,
                outputUriString = outputUriString,
                localCachePath = localCachePath,
                beforeSizeBytes = beforeSizeBytes,
                afterSizeBytes = afterSizeBytes,
                summaryLabel = summaryLabel
            )
        }
    }

    fun deleteMediaRecentFile(id: Long) {
        viewModelScope.launch {
            scanRepository.deleteMediaRecentFile(id)
        }
    }

    fun clearAllMediaRecentFiles() {
        viewModelScope.launch {
            scanRepository.clearAllMediaRecentFiles()
            _statusToast.value = "Cleared recent media files"
        }
    }

    private val _isDrawingPadOpen = MutableStateFlow(false)
    val isDrawingPadOpen: StateFlow<Boolean> = _isDrawingPadOpen.asStateFlow()

    private val _activeDrawingPadNote = MutableStateFlow<NoteRecord?>(null)
    val activeDrawingPadNote: StateFlow<NoteRecord?> = _activeDrawingPadNote.asStateFlow()

    private val _isFullScreenNoteEditorOpen = MutableStateFlow(false)
    val isFullScreenNoteEditorOpen: StateFlow<Boolean> = _isFullScreenNoteEditorOpen.asStateFlow()

    private val _activeEditorNote = MutableStateFlow<NoteRecord?>(null)
    val activeEditorNote: StateFlow<NoteRecord?> = _activeEditorNote.asStateFlow()

    fun openDrawingPad(note: NoteRecord? = null) {
        _activeDrawingPadNote.value = note
        _isDrawingPadOpen.value = true
    }

    fun closeDrawingPad() {
        _isDrawingPadOpen.value = false
        _activeDrawingPadNote.value = null
    }

    fun openFullScreenNoteEditor(note: NoteRecord? = null) {
        _activeEditorNote.value = note
        _isFullScreenNoteEditorOpen.value = true
    }

    fun closeFullScreenNoteEditor() {
        _isFullScreenNoteEditorOpen.value = false
        _activeEditorNote.value = null
    }

    val filteredScans: StateFlow<List<ScanRecord>> = combine(
        scanRepository.allScans,
        _searchQuery,
        _selectedFilter
    ) { scans, query, filter ->
        scans.filter { record ->
            val matchesFilter = when (filter) {
                ScanFilterCategory.ALL -> true
                ScanFilterCategory.QR_ONLY -> record.is2dMatrix
                ScanFilterCategory.BARCODE_ONLY -> !record.is2dMatrix
                ScanFilterCategory.FAVORITES -> record.isFavorite
            }
            val matchesQuery = query.isBlank() ||
                record.rawContent.contains(query, ignoreCase = true) ||
                record.displayTitle.contains(query, ignoreCase = true) ||
                record.formatName.contains(query, ignoreCase = true) ||
                record.contentType.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedFolder = MutableStateFlow("All")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _selectedTag = MutableStateFlow("All")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _isBulkSelectMode = MutableStateFlow(false)
    val isBulkSelectMode: StateFlow<Boolean> = _isBulkSelectMode.asStateFlow()

    private val _selectedDocIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDocIds: StateFlow<Set<Long>> = _selectedDocIds.asStateFlow()

    private val _ocrIsOnePagePreview = MutableStateFlow(false)
    val ocrIsOnePagePreview: StateFlow<Boolean> = _ocrIsOnePagePreview.asStateFlow()

    val filteredDocuments: StateFlow<List<DocumentRecord>> = combine(
        scanRepository.allDocuments,
        _searchQuery,
        _selectedDocCategory,
        _selectedDocSort,
        combine(_selectedFolder, _selectedTag) { folder, tag -> folder to tag }
    ) { docs, query, catFilter, sortOrder, (folderFilter, tagFilter) ->
        val filtered = docs.filter { doc ->
            val matchesCat = when (catFilter) {
                DocumentCategoryFilter.ALL -> true
                DocumentCategoryFilter.FAVORITES -> doc.favorite
                else -> doc.category.equals(catFilter.label, ignoreCase = true)
            }
            val matchesFolder = folderFilter == "All" ||
                doc.folderName.equals(folderFilter, ignoreCase = true)
            val matchesTag = tagFilter == "All" ||
                doc.tagsList.any { it.equals(tagFilter, ignoreCase = true) }
            val matchesQuery = query.isBlank() ||
                doc.name.contains(query, ignoreCase = true) ||
                doc.category.contains(query, ignoreCase = true) ||
                doc.ocrText.contains(query, ignoreCase = true) ||
                doc.folderName.contains(query, ignoreCase = true) ||
                doc.tagsCsv.contains(query, ignoreCase = true) ||
                doc.merchantName.contains(query, ignoreCase = true)
            matchesCat && matchesFolder && matchesTag && matchesQuery
        }
        when (sortOrder) {
            DocumentSortOrder.NEWEST -> filtered.sortedByDescending { it.createdAt }
            DocumentSortOrder.OLDEST -> filtered.sortedBy { it.createdAt }
            DocumentSortOrder.NAME_AZ -> filtered.sortedBy { it.name.lowercase() }
            DocumentSortOrder.PAGES -> filtered.sortedByDescending { it.pageCount }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredNotes: StateFlow<List<NoteRecord>> = combine(
        scanRepository.allNotes,
        _searchQuery
    ) { notes, query ->
        if (query.isBlank()) notes else notes.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true) ||
                it.categoryTag.contains(query, ignoreCase = true) ||
                it.tagsCsv.contains(query, ignoreCase = true) ||
                it.folderName.contains(query, ignoreCase = true) ||
                it.checklistJson.contains(query, ignoreCase = true) ||
                it.recognizedHandwritingText.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var lastScannedRaw: String? = null
    private var lastScanTimeMs: Long = 0L

    fun onBarcodeDetected(
        rawValue: String,
        format: Int,
        isPro: Boolean,
        onNewDetection: () -> Unit
    ) {
        val now = System.currentTimeMillis()
        if (_activeResultRecord.value != null) return
        if (rawValue == lastScannedRaw && (now - lastScanTimeMs) < 2200L) return

        lastScannedRaw = rawValue
        lastScanTimeMs = now
        onNewDetection()

        val (formatName, is2d) = mapBarcodeFormat(format)
        viewModelScope.launch {
            val saved = scanRepository.recordScan(
                rawContent = rawValue,
                formatName = formatName,
                is2dMatrix = is2d,
                isPro = isPro
            )
            _activeResultRecord.value = saved
        }
    }

    fun scanGalleryUri(
        context: Context,
        uri: Uri,
        isPro: Boolean,
        onSuccessFeedback: () -> Unit
    ) {
        runCatching {
            val image = InputImage.fromFilePath(context, uri)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_AZTEC,
                    Barcode.FORMAT_PDF417,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_CODABAR
                )
                .build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (first != null && first.rawValue != null) {
                        onSuccessFeedback()
                        val (formatName, is2d) = mapBarcodeFormat(first.format)
                        viewModelScope.launch {
                            val saved = scanRepository.recordScan(
                                rawContent = first.rawValue!!,
                                formatName = formatName,
                                is2dMatrix = is2d,
                                isPro = isPro
                            )
                            _activeResultRecord.value = saved
                        }
                    } else {
                        _statusToast.value = "No QR or barcode found in selected image."
                    }
                }
                .addOnFailureListener {
                    _statusToast.value = "Could not decode image."
                }
        }.onFailure {
            _statusToast.value = "Unable to open selected image."
        }
    }

    /**
     * Saves a newly scanned/cropped document into app-private storage + Room,
     * builds the PDF page-by-page with [PdfDocument] using the selected [pageSize], [fitMode],
     * [marginMode], and [qualityDpi], runs offline ML Kit OCR in the background so it is
     * immediately searchable, and invokes [onComplete].
     */
    fun createAndSaveDocument(
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
        onComplete: (DocumentRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            val savedDoc = scanRepository.createAndSaveDocument(
                context = context,
                name = name,
                category = category,
                filterMode = filterMode,
                pageBitmaps = pageBitmaps,
                pageSize = pageSize,
                fitMode = fitMode,
                marginMode = marginMode,
                qualityDpi = qualityDpi,
                pageLimit = pageLimit
            )
            // Automatically index OCR text in background for instant search
            launch {
                runCatching {
                    scanRepository.extractOcrTextForDocument(context, savedDoc)
                }
            }
            onComplete(savedDoc)
        }
    }

    fun reExportDocumentSettings(
        context: Context,
        doc: DocumentRecord,
        pageSize: PdfPageSize,
        fitMode: PdfFitMode,
        marginMode: PdfMarginMode,
        qualityDpi: PdfQualityDpi,
        filterMode: String = doc.filterMode
    ) {
        viewModelScope.launch {
            val updated = scanRepository.reExportDocumentPdf(
                context = context,
                doc = doc,
                pageSize = pageSize,
                fitMode = fitMode,
                marginMode = marginMode,
                qualityDpi = qualityDpi,
                filterMode = filterMode
            )
            val fileSizeStr = ScanRepository.formatFileSize(File(updated.pdfPath).length())
            _statusToast.value = "Re-exported PDF (${pageSize.label}, ${qualityDpi.label} • $fileSizeStr)"
        }
    }

    fun openDocumentViewer(doc: DocumentRecord) {
        _activeViewerDocId.value = doc.id
    }

    fun closeDocumentViewer() {
        _activeViewerDocId.value = null
    }

    fun renameDocument(docId: Long, newName: String) {
        viewModelScope.launch {
            scanRepository.renameDocument(docId, newName)
            _statusToast.value = "Document renamed"
        }
    }

    fun toggleDocumentFavorite(doc: DocumentRecord) {
        viewModelScope.launch {
            scanRepository.toggleDocumentFavorite(doc)
        }
    }

    fun setDocumentLocked(doc: DocumentRecord, locked: Boolean) {
        viewModelScope.launch {
            scanRepository.setDocumentLocked(doc.id, locked)
            _statusToast.value = if (locked) "Document locked with Biometrics" else "Document unlocked"
        }
    }

    fun deleteDocumentWithUndo(doc: DocumentRecord) {
        viewModelScope.launch {
            _recentlyDeletedDocument.value = doc
            if (_activeViewerDocId.value == doc.id) {
                _activeViewerDocId.value = null
            }
            scanRepository.deleteDocument(doc.id)
        }
    }

    fun undoDeleteDocument() {
        val deleted = _recentlyDeletedDocument.value ?: return
        viewModelScope.launch {
            scanRepository.restoreDeletedDocument(deleted)
            _recentlyDeletedDocument.value = null
            _statusToast.value = "Restored \"${deleted.name}\""
        }
    }

    fun dismissUndoBanner() {
        _recentlyDeletedDocument.value = null
    }

    fun reorderDocumentPage(
        context: Context,
        doc: DocumentRecord,
        fromIndex: Int,
        toIndex: Int
    ) {
        val pages = doc.pagePaths.toMutableList()
        if (fromIndex !in pages.indices || toIndex !in pages.indices || fromIndex == toIndex) return
        val moved = pages.removeAt(fromIndex)
        pages.add(toIndex, moved)
        viewModelScope.launch {
            scanRepository.updateDocumentPages(context, doc, pages)
            _statusToast.value = "Moved page ${fromIndex + 1} to ${toIndex + 1}"
        }
    }

    fun rotateDocumentPage(
        context: Context,
        doc: DocumentRecord,
        pageIndex: Int
    ) {
        viewModelScope.launch {
            scanRepository.rotateDocumentPage(context, doc, pageIndex)
            _statusToast.value = "Rotated page ${pageIndex + 1} by 90°"
        }
    }

    fun deleteDocumentPage(
        context: Context,
        doc: DocumentRecord,
        pageIndex: Int
    ) {
        val pages = doc.pagePaths.toMutableList()
        if (pages.size <= 1) {
            _statusToast.value = "A document must have at least 1 page. Delete the document instead."
            return
        }
        if (pageIndex !in pages.indices) return
        pages.removeAt(pageIndex)
        viewModelScope.launch {
            scanRepository.updateDocumentPages(context, doc, pages)
            _statusToast.value = "Deleted page ${pageIndex + 1}"
        }
    }

    fun addPageToDocument(
        context: Context,
        doc: DocumentRecord,
        newPageBitmap: Bitmap
    ) {
        if (doc.pageCount >= 50) {
            _statusToast.value = "Maximum 50 pages per document."
            return
        }
        viewModelScope.launch {
            val updatedPages = withContext(Dispatchers.IO) {
                val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
                val filtered = ScanRepository.applyFilterToBitmap(newPageBitmap, doc.filterMode)
                val newFile = File(docsDir, "doc_${doc.id}_p_${System.currentTimeMillis()}.jpg")
                FileOutputStream(newFile).use { out ->
                    filtered.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                doc.pagePaths + newFile.absolutePath
            }
            val updatedDoc = scanRepository.updateDocumentPages(context, doc, updatedPages)
            runCatching { scanRepository.extractOcrTextForDocument(context, updatedDoc) }
            _statusToast.value = "Added page ${updatedDoc.pageCount}"
        }
    }

    fun mergeWithDocument(
        context: Context,
        targetDoc: DocumentRecord,
        sourceDoc: DocumentRecord
    ) {
        if (targetDoc.id == sourceDoc.id) return
        viewModelScope.launch {
            val merged = scanRepository.mergeDocuments(context, targetDoc, sourceDoc)
            _statusToast.value = "Merged \"${sourceDoc.name}\" into \"${merged.name}\" (${merged.pageCount} pages)"
        }
    }

    fun launchExtractOcrSheet(
        context: Context,
        doc: DocumentRecord,
        onePagePreviewOnly: Boolean = false,
        pageIndex: Int = 0
    ) {
        _ocrSheetDoc.value = doc
        _ocrIsOnePagePreview.value = onePagePreviewOnly
        _ocrTextDraft.value = if (onePagePreviewOnly) {
            doc.ocrText.lines().take(18).joinToString("\n")
        } else {
            doc.ocrText
        }
        _isExtractingOcr.value = true
        viewModelScope.launch {
            val extracted = if (onePagePreviewOnly) {
                scanRepository.extractOnePageOcrPreview(context, doc, pageIndex)
            } else {
                scanRepository.extractOcrTextForDocument(context, doc)
            }
            _ocrTextDraft.value = extracted
            _isExtractingOcr.value = false
        }
    }

    fun updateOcrDraft(text: String) {
        _ocrTextDraft.value = text
    }

    fun saveOcrDraft() {
        val doc = _ocrSheetDoc.value ?: return
        val text = _ocrTextDraft.value
        viewModelScope.launch {
            scanRepository.saveEditedOcrText(doc.id, text)
            _ocrSheetDoc.value = null
            _statusToast.value = "Saved OCR text to document"
        }
    }

    fun closeOcrSheet() {
        _ocrSheetDoc.value = null
    }

    fun setSelectedFolder(folder: String) {
        _selectedFolder.value = folder
    }

    fun setSelectedTag(tag: String) {
        _selectedTag.value = tag
    }

    fun toggleBulkSelectMode(enabled: Boolean? = null) {
        val next = enabled ?: !_isBulkSelectMode.value
        _isBulkSelectMode.value = next
        if (!next) {
            _selectedDocIds.value = emptySet()
        }
    }

    fun toggleDocumentSelection(docId: Long) {
        val current = _selectedDocIds.value
        _selectedDocIds.value = if (current.contains(docId)) {
            current - docId
        } else {
            current + docId
        }
    }

    fun selectAllFilteredDocuments() {
        _selectedDocIds.value = filteredDocuments.value.map { it.id }.toSet()
    }

    fun clearBulkSelection() {
        _selectedDocIds.value = emptySet()
        _isBulkSelectMode.value = false
    }

    fun bulkDeleteSelectedDocuments() {
        val ids = _selectedDocIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { scanRepository.deleteDocument(it) }
            _selectedDocIds.value = emptySet()
            _isBulkSelectMode.value = false
            _statusToast.value = "Deleted ${ids.size} documents"
        }
    }

    fun bulkMoveSelectedToFolder(folderName: String) {
        val ids = _selectedDocIds.value
        if (ids.isEmpty()) return
        val cleanFolder = folderName.trim()
        viewModelScope.launch {
            val docs = allDocuments.value.filter { it.id in ids }
            docs.forEach { doc ->
                scanRepository.updateDocumentRecord(doc.copy(folderName = cleanFolder))
            }
            _selectedDocIds.value = emptySet()
            _isBulkSelectMode.value = false
            _statusToast.value = if (cleanFolder.isBlank()) {
                "Cleared folder on ${docs.size} documents"
            } else {
                "Moved ${docs.size} documents to \"$cleanFolder\""
            }
        }
    }

    fun bulkMergeSelectedDocuments(context: Context, maxFilesAllowed: Int) {
        val ids = _selectedDocIds.value
        val docs = allDocuments.value.filter { it.id in ids }
        if (docs.size < 2) {
            _statusToast.value = "Select at least 2 documents to merge."
            return
        }
        val capped = docs.take(maxFilesAllowed)
        viewModelScope.launch {
            val merged = scanRepository.mergeMultipleDocuments(context, capped)
            _selectedDocIds.value = emptySet()
            _isBulkSelectMode.value = false
            if (merged != null) {
                _statusToast.value = "Merged ${capped.size} documents into \"${merged.name}\" (${merged.pageCount} pages)"
            }
        }
    }

    fun bulkExportSelectedAsZip(
        context: Context,
        saveToDownloads: Boolean,
        onShareIntentReady: (Intent) -> Unit = {}
    ) {
        val ids = _selectedDocIds.value
        val docs = if (ids.isNotEmpty()) {
            allDocuments.value.filter { it.id in ids }
        } else {
            filteredDocuments.value
        }
        if (docs.isEmpty()) {
            _statusToast.value = "No documents to export."
            return
        }
        viewModelScope.launch {
            if (saveToDownloads) {
                val path = com.example.data.DocumentSmartTools.saveBatchZipToDownloads(context, docs)
                if (path != null) {
                    _statusToast.value = "Saved ZIP (${docs.size} PDFs) to $path"
                } else {
                    _statusToast.value = "Could not save ZIP to Downloads."
                }
            } else {
                val intent = com.example.data.DocumentSmartTools.createShareBatchZipIntent(context, docs)
                if (intent != null) {
                    onShareIntentReady(Intent.createChooser(intent, "Export Documents ZIP"))
                } else {
                    _statusToast.value = "Could not create ZIP archive."
                }
            }
            _selectedDocIds.value = emptySet()
            _isBulkSelectMode.value = false
        }
    }

    fun applySmartFilterAndSliders(
        context: Context,
        doc: DocumentRecord,
        filterMode: String,
        brightnessDelta: Float,
        contrastScale: Float,
        sharpenAmount: Float,
        batchApplyAllPages: Boolean = false,
        batchAllPages: Boolean = batchApplyAllPages,
        targetPageIndex: Int = 0
    ) {
        val applyAll = batchApplyAllPages || batchAllPages
        viewModelScope.launch {
            val updated = scanRepository.applySmartScanFilterAndSlidersToDocument(
                context = context,
                doc = doc,
                filterMode = filterMode,
                brightnessDelta = brightnessDelta,
                contrastScale = contrastScale,
                sharpenAmount = sharpenAmount,
                batchApplyAllPages = applyAll,
                targetPageIndex = targetPageIndex
            )
            _statusToast.value = if (applyAll) {
                "Applied $filterMode across all ${updated.pageCount} pages"
            } else {
                "Applied $filterMode to page ${targetPageIndex + 1}"
            }
        }
    }

    fun saveDocumentOrganizationAndExpiry(
        context: Context,
        doc: DocumentRecord,
        folderName: String,
        tagsCsv: String,
        expiryEpochMs: Long?
    ) {
        viewModelScope.launch {
            val resolvedExpiry = expiryEpochMs ?: 0L
            val updated = doc.copy(
                folderName = folderName.trim(),
                tagsCsv = tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(","),
                expiryEpochMs = resolvedExpiry
            )
            scanRepository.updateDocumentRecord(updated)
            if (resolvedExpiry > 0L) {
                com.example.data.DocumentSmartTools.ExpiryReminderScheduler.scheduleAndNotifyExpiryMilestones(
                    context = context,
                    doc = updated
                )
                val badge = com.example.data.DocumentSmartTools.ExpiryReminderScheduler.formatExpiryBadge(resolvedExpiry)
                _statusToast.value = "Saved folder, tags & expiry reminder ($badge • 30d/7d/1d alerts)"
            } else {
                _statusToast.value = "Updated folder & tags"
            }
        }
    }

    fun saveReceiptMetadata(
        doc: DocumentRecord,
        merchantName: String,
        receiptDateIso: String,
        receiptTotalAmount: Double?,
        expenseCategory: String
    ) {
        viewModelScope.launch {
            val resolvedAmount = receiptTotalAmount ?: 0.0
            val updated = doc.copy(
                category = "Receipt",
                merchantName = merchantName.trim(),
                receiptDateIso = receiptDateIso.trim(),
                receiptTotalAmount = resolvedAmount,
                expenseCategory = expenseCategory.trim().ifBlank { "General" }
            )
            scanRepository.updateDocumentRecord(updated)
            _statusToast.value = "Saved receipt (${merchantName.ifBlank { doc.name }} • $${String.format(java.util.Locale.US, "%.2f", resolvedAmount)})"
        }
    }

    fun saveReceiptExpenseMetadata(
        doc: DocumentRecord,
        merchant: String,
        dateIso: String,
        totalAmount: Double,
        expenseCategory: String
    ) {
        saveReceiptMetadata(
            doc = doc,
            merchantName = merchant,
            receiptDateIso = dateIso,
            receiptTotalAmount = totalAmount,
            expenseCategory = expenseCategory
        )
    }

    fun exportReceiptsCsv(
        context: Context,
        onShareIntentReady: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            val intent = com.example.data.DocumentSmartTools.exportExpensesToCsvIntent(
                context = context,
                documents = allDocuments.value
            )
            if (intent != null) {
                onShareIntentReady(Intent.createChooser(intent, "Export Receipts & Expenses CSV"))
            } else {
                _statusToast.value = "No receipts or documents to export."
            }
        }
    }

    fun protectDocumentWithPassword(
        context: Context,
        doc: DocumentRecord,
        password: String
    ) {
        if (password.isBlank()) {
            _statusToast.value = "Password cannot be empty."
            return
        }
        viewModelScope.launch {
            val pdfFile = File(doc.pdfPath)
            val ok = com.example.data.DocumentSmartTools.protectPdfWithPassword(
                context = context,
                sourcePdfFile = pdfFile,
                userPassword = password,
                ownerPassword = password,
                outputPdfFile = pdfFile
            )
            if (ok) {
                scanRepository.updateDocumentRecord(doc.copy(isPasswordProtected = true))
                _statusToast.value = "PDF encrypted with 128-bit password protection"
            } else {
                _statusToast.value = "Could not encrypt PDF."
            }
        }
    }

    fun unlockPasswordProtectedDocument(
        context: Context,
        doc: DocumentRecord,
        password: String
    ) {
        viewModelScope.launch {
            val pdfFile = File(doc.pdfPath)
            val ok = com.example.data.DocumentSmartTools.unlockPasswordProtectedPdf(
                context = context,
                sourcePdfFile = pdfFile,
                password = password,
                outputPdfFile = pdfFile
            )
            if (ok) {
                scanRepository.updateDocumentRecord(doc.copy(isPasswordProtected = false))
                _statusToast.value = "PDF password removed & unlocked"
            } else {
                _statusToast.value = "Incorrect password or unable to unlock PDF."
            }
        }
    }

    fun unlockDocumentWithPassword(
        context: Context,
        doc: DocumentRecord,
        password: String
    ) = unlockPasswordProtectedDocument(context, doc, password)

    fun shareDocumentPdf(
        context: Context,
        doc: DocumentRecord,
        onShareIntentReady: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            val intent = scanRepository.createSharePdfIntent(context, doc)
            if (intent != null) {
                onShareIntentReady(Intent.createChooser(intent, "Share PDF Document"))
            } else {
                _statusToast.value = "Unable to prepare PDF for sharing."
            }
        }
    }

    fun saveDocumentToPhone(context: Context, doc: DocumentRecord) {
        viewModelScope.launch {
            val savedPath = scanRepository.savePdfToPhoneDownloads(context, doc)
            if (savedPath != null) {
                _statusToast.value = "Saved PDF to $savedPath"
            } else {
                _statusToast.value = "Could not save PDF to Downloads."
            }
        }
    }

    fun saveNote(
        title: String,
        content: String,
        categoryTag: String = "Measurement",
        colorHex: String = "#CCFF00",
        isPinned: Boolean = false,
        existingId: Long = 0L,
        noteType: String = "TEXT",
        checklistJson: String = "",
        tagsCsv: String = "",
        folderName: String = "",
        isLocked: Boolean = false,
        reminderEpochMs: Long = 0L,
        attachedImagePathsJoined: String = "",
        paperStyleId: String = "PLAIN_WHITE",
        strokesJson: String = "",
        recognizedHandwritingText: String = "",
        silentAutoSave: Boolean = false,
        onSaved: (NoteRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            val saved = scanRepository.saveNote(
                title = title,
                content = content,
                categoryTag = categoryTag,
                colorHex = colorHex,
                isPinned = isPinned,
                existingId = existingId,
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
            if (!silentAutoSave) {
                _statusToast.value = "Saved note \"${saved.title}\""
            }
            onSaved(saved)
        }
    }

    fun saveDrawingNote(
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
        isLocked: Boolean = false,
        silentAutoSave: Boolean = false,
        onSaved: (NoteRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            val saved = scanRepository.saveDrawingPngNote(
                context = context,
                title = title,
                paperColorHex = paperColorHex,
                bitmap = bitmap,
                isPinned = isPinned,
                existingId = existingId,
                paperStyleId = paperStyleId,
                strokesJson = strokesJson,
                recognizedHandwritingText = recognizedHandwritingText,
                tagsCsv = tagsCsv,
                folderName = folderName,
                isLocked = isLocked
            )
            if (!silentAutoSave) {
                _statusToast.value = "Saved sketch \"${saved.title}\""
            }
            onSaved(saved)
        }
    }

    fun attachImageUriToNote(
        context: Context,
        uri: Uri,
        onCopiedPath: (String) -> Unit
    ) {
        viewModelScope.launch {
            val path = scanRepository.copyUriToNoteImages(context, uri)
            if (path != null) {
                onCopiedPath(path)
                _statusToast.value = "Inserted photo into note"
            } else {
                _statusToast.value = "Could not load selected photo."
            }
        }
    }

    fun exportNotePdf(
        context: Context,
        note: NoteRecord,
        pageSize: PdfPageSize = PdfPageSize.A4,
        onShareIntentReady: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            val intent = scanRepository.exportNoteToPdfIntent(context, note, pageSize)
            if (intent != null) {
                _statusToast.value = "Exported PDF (${pageSize.label}) to Downloads/Dyne"
                onShareIntentReady(Intent.createChooser(intent, "Export Note PDF"))
            } else {
                _statusToast.value = "Unable to export PDF."
            }
        }
    }

    fun exportNoteMarkdownOrTxt(
        context: Context,
        note: NoteRecord,
        asMarkdown: Boolean,
        onShareIntentReady: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            val intent = scanRepository.exportNoteTextFileIntent(context, note, asMarkdown)
            if (intent != null) {
                val fmt = if (asMarkdown) "Markdown (.md)" else "TXT (.txt)"
                _statusToast.value = "Exported $fmt to Downloads/Dyne"
                onShareIntentReady(Intent.createChooser(intent, "Export Note $fmt"))
            } else {
                _statusToast.value = "Unable to export note file."
            }
        }
    }

    fun setNoteLocked(note: NoteRecord, isLocked: Boolean) {
        viewModelScope.launch {
            scanRepository.setNoteLocked(note, isLocked)
            _statusToast.value = if (isLocked) "Note locked with biometrics" else "Note unlocked"
        }
    }

    fun shareDrawingPng(
        context: Context,
        note: NoteRecord,
        onShareIntentReady: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            val intent = scanRepository.createShareDrawingIntent(context, note)
            if (intent != null) {
                onShareIntentReady(Intent.createChooser(intent, "Share Drawing PNG"))
            } else {
                _statusToast.value = "Unable to prepare PNG for sharing."
            }
        }
    }

    fun exportDrawingPngToPhone(context: Context, note: NoteRecord) {
        viewModelScope.launch {
            val path = scanRepository.saveDrawingToPhoneGallery(context, note)
            if (path != null) {
                _statusToast.value = "Exported PNG to $path"
            } else {
                _statusToast.value = "Could not export PNG to Pictures."
            }
        }
    }

    fun toggleNotePinned(note: NoteRecord) {
        viewModelScope.launch {
            scanRepository.toggleNotePinned(note)
        }
    }

    fun selectRecordForSheet(record: ScanRecord?) {
        _activeResultRecord.value = record
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ScanFilterCategory) {
        _selectedFilter.value = filter
    }

    fun setDocCategoryFilter(category: DocumentCategoryFilter) {
        _selectedDocCategory.value = category
    }

    fun setDocSortOrder(sortOrder: DocumentSortOrder) {
        _selectedDocSort.value = sortOrder
    }

    fun toggleTorch() {
        _torchEnabled.value = !_torchEnabled.value
    }

    fun toggleScanFavorite(record: ScanRecord) {
        viewModelScope.launch {
            scanRepository.toggleScanFavorite(record)
            if (_activeResultRecord.value?.id == record.id) {
                _activeResultRecord.value = record.copy(isFavorite = !record.isFavorite)
            }
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            scanRepository.deleteScan(id)
            if (_activeResultRecord.value?.id == id) {
                _activeResultRecord.value = null
            }
        }
    }

    fun toggleNoteFavorite(note: NoteRecord) {
        viewModelScope.launch {
            scanRepository.toggleNoteFavorite(note)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            scanRepository.deleteNote(id)
            _statusToast.value = "Note deleted"
        }
    }

    fun exportCsv(context: Context, onShareIntentReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val intent = scanRepository.exportToCsvShareIntent(context)
            if (intent != null) {
                onShareIntentReady(Intent.createChooser(intent, "Export Scan History CSV"))
            } else {
                _statusToast.value = "Scan history is empty."
            }
        }
    }

    fun showToast(message: String) {
        _statusToast.value = message
    }

    fun clearToast() {
        _statusToast.value = null
    }

    companion object {
        fun mapBarcodeFormat(format: Int): Pair<String, Boolean> {
            return when (format) {
                Barcode.FORMAT_QR_CODE -> "QR Code" to true
                Barcode.FORMAT_DATA_MATRIX -> "Data Matrix" to true
                Barcode.FORMAT_AZTEC -> "Aztec" to true
                Barcode.FORMAT_PDF417 -> "PDF417" to true
                Barcode.FORMAT_EAN_13 -> "EAN-13" to false
                Barcode.FORMAT_EAN_8 -> "EAN-8" to false
                Barcode.FORMAT_UPC_A -> "UPC-A" to false
                Barcode.FORMAT_UPC_E -> "UPC-E" to false
                Barcode.FORMAT_CODE_128 -> "Code 128" to false
                Barcode.FORMAT_CODE_39 -> "Code 39" to false
                Barcode.FORMAT_CODE_93 -> "Code 93" to false
                Barcode.FORMAT_ITF -> "ITF" to false
                Barcode.FORMAT_CODABAR -> "Codabar" to false
                else -> "Barcode" to false
            }
        }
    }

    class Factory(private val scanRepository: ScanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScanViewModel(scanRepository) as T
        }
    }
}
