package com.example.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.QuotaAllowanceChip
import com.example.billing.QuotaType
import com.example.data.PdfFitMode
import com.example.data.PdfMarginMode
import com.example.data.PdfPageSize
import com.example.data.PdfQualityDpi
import com.example.data.ScanRepository
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassSurface
import com.example.ui.components.GlassSheet
import com.example.ui.components.HazeState
import com.example.ui.components.pressScaleClickable
import com.example.ui.scan.ScanViewModel
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

import com.example.ui.components.ProBadge
import com.example.billing.DocumentPackFeature
import com.example.billing.LocalFeatureGate
import com.example.data.DocumentSmartTools
import com.example.data.FormFillSnippets
import com.example.data.HeaderFooterConfig
import com.example.data.NormalizedCropRect
import com.example.data.PageNumberConfig
import com.example.data.PageNumberFormat
import com.example.data.PageNumberPosition
import com.example.data.PdfCompressResult
import com.example.data.PdfCompressionLevel
import com.example.data.PdfOfficialStamp
import com.example.data.SavedSignatureItem
import com.example.data.StampPlacement
import com.example.data.WatermarkConfig
import com.example.data.WatermarkPosition

enum class PdfAnnotationTool(val label: String, val isProTool: Boolean = false) {
    VIEW_PAN("Pan/Zoom"),
    PEN("Pen"),
    HIGHLIGHTER("Highlight"),
    TEXT_BOX("Text Box"),
    REDACT("Redact", isProTool = true),
    RECTANGLE("Rectangle"),
    ARROW("Arrow"),
    CHECKMARK("Check"),
    SIGNATURE("Signature")
}

enum class PdfFontChoice(val label: String, val typeface: Typeface, val composeFont: FontFamily) {
    SANS("Sans", Typeface.SANS_SERIF, FontFamily.SansSerif),
    SERIF("Serif", Typeface.SERIF, FontFamily.Serif),
    MONO("Mono", Typeface.MONOSPACE, FontFamily.Monospace)
}

sealed class PdfAnnotationItem {
    data class FreehandStroke(
        val pointsNormalized: List<PointF>,
        val colorArgb: Int,
        val strokeWidthRatio: Float,
        val isHighlighter: Boolean
    ) : PdfAnnotationItem()

    data class TextBoxItem(
        val text: String,
        val xNorm: Float,
        val yNorm: Float,
        val fontChoice: PdfFontChoice,
        val fontSizeSp: Float,
        val colorArgb: Int
    ) : PdfAnnotationItem()

    data class ShapeItem(
        val type: PdfAnnotationTool, // RECTANGLE, ARROW, CHECKMARK
        val startNorm: PointF,
        val endNorm: PointF,
        val colorArgb: Int,
        val strokeWidthRatio: Float = 0.006f
    ) : PdfAnnotationItem()

    data class SignatureStamp(
        val xNorm: Float,
        val yNorm: Float,
        val widthNorm: Float,
        val heightNorm: Float,
        val pathsNormalized: List<List<PointF>>,
        val colorArgb: Int
    ) : PdfAnnotationItem()

    data class OfficialStampAnnotation(
        val stamp: PdfOfficialStamp,
        val xNorm: Float,
        val yNorm: Float,
        val scale: Float = 1f
    ) : PdfAnnotationItem()

    data class RedactionBoxItem(
        val startNorm: PointF,
        val endNorm: PointF
    ) : PdfAnnotationItem()
}

data class EditablePdfPage(
    val id: Long = System.nanoTime(),
    val baseBitmap: Bitmap,
    val rotationDegrees: Int = 0,
    val annotations: List<PdfAnnotationItem> = emptyList()
)

private data class PdfEditorSnapshot(
    val docTitle: String,
    val pages: List<EditablePdfPage>,
    val selectedPageIndex: Int,
    val pageSize: PdfPageSize
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfToolsScreen(
    scanViewModel: ScanViewModel,
    isUnlimitedMergeUnlocked: Boolean,
    hazeState: HazeState?,
    onRequestUnlockUnlimitedMerge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val scope = rememberCoroutineScope()

    var documentTitle by remember { mutableStateOf("Edited Document") }
    val pages = remember { mutableStateListOf<EditablePdfPage>() }
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var selectedPageSize by remember { mutableStateOf(PdfPageSize.defaultForLocale()) }
    var selectedFitMode by remember { mutableStateOf(PdfFitMode.FIT) }
    var selectedMarginMode by remember { mutableStateOf(PdfMarginMode.SMALL_10MM) }
    var selectedQualityDpi by remember { mutableStateOf(PdfQualityDpi.HIGH_200) }

    // Undo / Redo stacks
    val undoStack = remember { mutableStateListOf<PdfEditorSnapshot>() }
    val redoStack = remember { mutableStateListOf<PdfEditorSnapshot>() }

    // Active annotation tool & styling
    var activeTool by remember { mutableStateOf(PdfAnnotationTool.VIEW_PAN) }
    val colorChoices = remember {
        listOf(
            Color(0xFFEF4444), // Red
            Color(0xFF3B82F6), // Blue
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFF111827), // Ink Black
            Color(0xFF8B5CF6)  // Purple
        )
    }
    var selectedColor by remember { mutableStateOf(colorChoices.first()) }
    var selectedFont by remember { mutableStateOf(PdfFontChoice.SANS) }
    var selectedFontSizeSp by remember { mutableFloatStateOf(20f) }

    // Live gesture drawing state
    val currentStrokePoints = remember { mutableStateListOf<PointF>() }
    var shapeDragStart by remember { mutableStateOf<PointF?>(null) }
    var shapeDragEnd by remember { mutableStateOf<PointF?>(null) }

    // Pinch-to-zoom & pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Text box creation sheet state
    var pendingTextPlacementNorm by remember { mutableStateOf<PointF?>(null) }
    var textBoxInput by remember { mutableStateOf("") }

    // Signature pad sheet & saved signature paths (up to 5 saved signatures & initials)
    var showSignaturePadSheet by remember { mutableStateOf(false) }
    val savedSignaturePaths = remember { mutableStateListOf<List<PointF>>() }
    val savedSignatureLibrary = remember { mutableStateListOf<SavedSignatureItem>() }
    var signatureScaleFactor by remember { mutableFloatStateOf(0.28f) }

    // Form-fill snippets (name, address, date)
    var formSnippets by remember { mutableStateOf(FormFillSnippets()) }
    var showEditSnippetsRow by remember { mutableStateOf(false) }

    // Premium PDF Pack sheet states & configurations
    val featureGate = LocalFeatureGate.current
    var showCompressSheet by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }
    var showWatermarkSheet by remember { mutableStateOf(false) }
    var showPageNumbersHeaderSheet by remember { mutableStateOf(false) }
    var showStampsSheet by remember { mutableStateOf(false) }
    var showCropSheet by remember { mutableStateOf(false) }

    var watermarkConfig by remember { mutableStateOf(WatermarkConfig()) }
    var watermarkImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageNumberConfig by remember { mutableStateOf(PageNumberConfig()) }
    var headerFooterConfig by remember { mutableStateOf(HeaderFooterConfig()) }
    var activeStampToPlace by remember { mutableStateOf<PdfOfficialStamp?>(null) }
    var pdfPasswordToApply by remember { mutableStateOf("") }

    // Status message
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        savedSignatureLibrary.clear()
        savedSignatureLibrary.addAll(DocumentSmartTools.loadSavedSignatures(context))
        if (savedSignatureLibrary.isNotEmpty() && savedSignaturePaths.isEmpty()) {
            savedSignaturePaths.addAll(savedSignatureLibrary.first().pathsNormalized)
        }
        formSnippets = DocumentSmartTools.loadFormFillSnippets(context)
    }

    fun requirePdfPackFeature(feature: DocumentPackFeature, onAllowed: () -> Unit) {
        if (isUnlimitedMergeUnlocked || featureGate == null) {
            onAllowed()
        } else {
            featureGate.requireDocumentPack(feature, onAllowed)
        }
    }

    fun pushUndoSnapshot() {
        undoStack.add(
            PdfEditorSnapshot(
                docTitle = documentTitle,
                pages = pages.map { it.copy(annotations = it.annotations.toList()) },
                selectedPageIndex = selectedPageIndex,
                pageSize = selectedPageSize
            )
        )
        if (undoStack.size > 25) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun autoSaveDraft() {
        val snapshotPages = pages.toList()
        val titleCopy = documentTitle
        val sizeCopy = selectedPageSize
        scope.launch(Dispatchers.IO) {
            runCatching {
                val draftDir = File(context.filesDir, "pdf_drafts").apply { mkdirs() }
                draftDir.listFiles()?.forEach { if (it.name.startsWith("draft_page_")) it.delete() }
                snapshotPages.take(20).forEachIndexed { idx, p ->
                    val rendered = renderEditablePageToBitmap(p)
                    val out = File(draftDir, "draft_page_${idx + 1}.jpg")
                    FileOutputStream(out).use { stream ->
                        rendered.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                    }
                }
                File(draftDir, "draft_meta.txt").writeText("$titleCopy|${sizeCopy.id}|${snapshotPages.size}")
            }
        }
    }

    // Load auto-saved draft or create a starter page on first open
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val draftDir = File(context.filesDir, "pdf_drafts")
            val metaFile = File(draftDir, "draft_meta.txt")
            if (metaFile.exists()) {
                val parts = metaFile.readText().split("|")
                val savedTitle = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "Draft PDF"
                val savedSize = PdfPageSize.fromId(parts.getOrNull(1))
                val pageFiles = (1..20).mapNotNull { idx ->
                    File(draftDir, "draft_page_$idx.jpg").takeIf { it.exists() }
                }
                val loadedPages = pageFiles.mapNotNull { file ->
                    BitmapFactory.decodeFile(file.absolutePath)?.let { EditablePdfPage(baseBitmap = it) }
                }
                if (loadedPages.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        documentTitle = savedTitle
                        selectedPageSize = savedSize
                        pages.clear()
                        pages.addAll(loadedPages)
                        selectedPageIndex = 0
                        statusBanner = "Restored auto-saved draft (${loadedPages.size} pages)"
                    }
                    return@withContext
                }
            }
            val starterBmp = ScanRepository.generateSampleDocumentBitmap(
                title = "Dyne PDF Workspace",
                category = "PDF Tools",
                filterMode = "Original Color",
                pageNumber = 1
            )
            withContext(Dispatchers.Main) {
                pages.clear()
                pages.add(EditablePdfPage(baseBitmap = starterBmp))
                selectedPageIndex = 0
            }
        }
    }

    // 1. Open single PDF launcher (no storage permission needed)
    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isBusy = true
            scope.launch {
                val imported = renderPdfUriToPages(context, uri, maxPages = if (isUnlimitedMergeUnlocked) 50 else 25)
                isBusy = false
                if (imported.isNotEmpty()) {
                    pushUndoSnapshot()
                    pages.clear()
                    pages.addAll(imported)
                    selectedPageIndex = 0
                    documentTitle = "Imported PDF"
                    zoomScale = 1f
                    panOffset = Offset.Zero
                    autoSaveDraft()
                    statusBanner = "Imported ${imported.size} ${if (imported.size == 1) "page" else "pages"} via PdfRenderer"
                } else {
                    statusBanner = "Could not render pages from the selected PDF."
                }
            }
        }
    }

    // 2. Merge multiple PDFs launcher (up to 3 files Free, unlimited with Premium)
    val mergePdfsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (uris.size > 3 && !isUnlimitedMergeUnlocked) {
                statusBanner = "Free tier merges up to 3 PDF files at once. Unlock Premium for unlimited merges."
                onRequestUnlockUnlimitedMerge()
                return@rememberLauncherForActivityResult
            }
            isBusy = true
            scope.launch {
                val mergedPages = mutableListOf<EditablePdfPage>()
                val maxFiles = if (isUnlimitedMergeUnlocked) uris.size else min(uris.size, 3)
                for (uri in uris.take(maxFiles)) {
                    val filePages = renderPdfUriToPages(context, uri, maxPages = 25)
                    mergedPages.addAll(filePages)
                }
                isBusy = false
                if (mergedPages.isNotEmpty()) {
                    pushUndoSnapshot()
                    pages.addAll(mergedPages)
                    selectedPageIndex = (pages.size - 1).coerceAtLeast(0)
                    autoSaveDraft()
                    statusBanner = "Merged ${uris.take(maxFiles).size} PDF files (${pages.size} total pages)"
                } else {
                    statusBanner = "No valid PDF pages found to merge."
                }
            }
        }
    }

    // 3. Images to PDF launcher (System Photo Picker, zero permissions)
    val imagesToPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isBusy = true
            scope.launch {
                val decodedPages = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        ScanRepository.decodeUriWithExifRotation(context, uri)?.let { bmp ->
                            EditablePdfPage(baseBitmap = bmp)
                        }
                    }
                }
                isBusy = false
                if (decodedPages.isNotEmpty()) {
                    pushUndoSnapshot()
                    pages.addAll(decodedPages)
                    selectedPageIndex = (pages.size - decodedPages.size).coerceAtLeast(0)
                    autoSaveDraft()
                    statusBanner = "Added ${decodedPages.size} image(s) with ${selectedPageSize.label} sizing"
                }
            }
        }
    }

    // 4. Watermark Image picker (Zero permission Photo Picker)
    val watermarkImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    ScanRepository.decodeUriWithExifRotation(context, uri)
                }
                if (bmp != null) {
                    watermarkImageBitmap = bmp
                    statusBanner = "Loaded watermark image (${bmp.width}×${bmp.height})"
                }
            }
        }
    }

    val safePageIndex = selectedPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val currentPage = pages.getOrNull(safePageIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Honest Flattened Pages Notice Banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.surface)
                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("pdf_tools_flattened_notice")
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Flattened PDF Notice",
                tint = theme.accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Pages are flattened, so you can organize, annotate and sign, but not rewrite the original text.",
                style = type.caption,
                color = theme.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        if (statusBanner != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.accent.copy(alpha = 0.14f))
                    .border(1.dp, theme.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = statusBanner!!,
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Dismiss",
                    style = type.caption,
                    color = theme.textSecondary,
                    modifier = Modifier.pressScaleClickable { statusBanner = null }
                )
            }
        }

        // Top File Actions Row: Import PDF, Merge PDFs, Images to PDF
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Import PDF",
                icon = Icons.Default.FileOpen,
                accentFill = theme.accent,
                onClick = {
                    openPdfLauncher.launch(arrayOf("application/pdf"))
                },
                modifier = Modifier.weight(1f),
                testTag = "pdf_import_button"
            )
            GlassButton(
                text = if (isUnlimitedMergeUnlocked) "Merge PDFs" else "Merge (≤3)",
                icon = Icons.Default.MergeType,
                onClick = {
                    mergePdfsLauncher.launch(arrayOf("application/pdf"))
                },
                modifier = Modifier.weight(1f),
                testTag = "pdf_merge_button"
            )
            GlassButton(
                text = "Images → PDF",
                icon = Icons.Default.AddPhotoAlternate,
                onClick = {
                    imagesToPdfLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f),
                testTag = "pdf_images_to_pdf_button"
            )
        }

        // Document Title + Undo / Redo Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = documentTitle,
                onValueChange = {
                    documentTitle = it
                    autoSaveDraft()
                },
                label = { Text("PDF Title") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (undoStack.isNotEmpty()) {
                        val last = undoStack.removeAt(undoStack.lastIndex)
                        redoStack.add(
                            PdfEditorSnapshot(
                                docTitle = documentTitle,
                                pages = pages.map { it.copy(annotations = it.annotations.toList()) },
                                selectedPageIndex = selectedPageIndex,
                                pageSize = selectedPageSize
                            )
                        )
                        documentTitle = last.docTitle
                        pages.clear()
                        pages.addAll(last.pages)
                        selectedPageIndex = last.selectedPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                        selectedPageSize = last.pageSize
                        autoSaveDraft()
                    }
                },
                enabled = undoStack.isNotEmpty(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(theme.surface)
                    .testTag("pdf_undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (undoStack.isNotEmpty()) theme.accent else theme.textSecondary.copy(alpha = 0.4f)
                )
            }

            IconButton(
                onClick = {
                    if (redoStack.isNotEmpty()) {
                        val next = redoStack.removeAt(redoStack.lastIndex)
                        undoStack.add(
                            PdfEditorSnapshot(
                                docTitle = documentTitle,
                                pages = pages.map { it.copy(annotations = it.annotations.toList()) },
                                selectedPageIndex = selectedPageIndex,
                                pageSize = selectedPageSize
                            )
                        )
                        documentTitle = next.docTitle
                        pages.clear()
                        pages.addAll(next.pages)
                        selectedPageIndex = next.selectedPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                        selectedPageSize = next.pageSize
                        autoSaveDraft()
                    }
                },
                enabled = redoStack.isNotEmpty(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(theme.surface)
                    .testTag("pdf_redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (redoStack.isNotEmpty()) theme.accent else theme.textSecondary.copy(alpha = 0.4f)
                )
            }
        }

        // Page-Size Selector Strip (for Images to PDF and PDF Export)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "EXPORT PAGE SIZE: ${selectedPageSize.label.uppercase()} (${selectedPageSize.subtitle})",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )
                if (zoomScale > 1.02f) {
                    Text(
                        text = "Zoom ${(zoomScale * 100).roundToInt()}% • Reset",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent,
                        modifier = Modifier.pressScaleClickable {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                PdfPageSize.entries.forEach { sizeOption ->
                    val isSelected = selectedPageSize == sizeOption
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accent else theme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) theme.accent else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .pressScaleClickable(
                                testTag = "pdf_editor_size_${sizeOption.id}",
                                onClick = {
                                    selectedPageSize = sizeOption
                                    autoSaveDraft()
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sizeOption.label,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) BentoTextBlack else theme.textPrimary
                        )
                    }
                }
            }
        }

        // Interactive Page Canvas with Pinch-Zoom & Annotations
        if (currentPage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0B0E14))
                    .border(1.5.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = remember(currentPage.baseBitmap, currentPage.rotationDegrees) {
                    rotateBitmapIfNeeded(currentPage.baseBitmap, currentPage.rotationDegrees)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                        .pointerInput(activeTool) {
                            if (activeTool == PdfAnnotationTool.VIEW_PAN) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                                    zoomScale = newScale
                                    panOffset = if (newScale > 1f) {
                                        Offset(
                                            x = (panOffset.x + pan.x).coerceIn(-350f, 350f),
                                            y = (panOffset.y + pan.y).coerceIn(-350f, 350f)
                                        )
                                    } else {
                                        Offset.Zero
                                    }
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "PDF Page ${safePageIndex + 1}",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )

                    // Annotation Overlay Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activeTool, safePageIndex, selectedColor, signatureScaleFactor) {
                                if (activeTool == PdfAnnotationTool.TEXT_BOX ||
                                    activeTool == PdfAnnotationTool.CHECKMARK ||
                                    activeTool == PdfAnnotationTool.SIGNATURE
                                ) {
                                    detectTapGestures { tapOffset ->
                                        val nx = (tapOffset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0.05f, 0.92f)
                                        val ny = (tapOffset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0.05f, 0.92f)
                                        when (activeTool) {
                                            PdfAnnotationTool.TEXT_BOX -> {
                                                pendingTextPlacementNorm = PointF(nx, ny)
                                                textBoxInput = ""
                                            }
                                            PdfAnnotationTool.CHECKMARK -> {
                                                pushUndoSnapshot()
                                                val checkItem = PdfAnnotationItem.ShapeItem(
                                                    type = PdfAnnotationTool.CHECKMARK,
                                                    startNorm = PointF(nx, ny),
                                                    endNorm = PointF((nx + 0.08f).coerceAtMost(0.96f), (ny + 0.06f).coerceAtMost(0.96f)),
                                                    colorArgb = selectedColor.toArgb()
                                                )
                                                val p = pages[safePageIndex]
                                                pages[safePageIndex] = p.copy(annotations = p.annotations + checkItem)
                                                autoSaveDraft()
                                            }
                                            PdfAnnotationTool.SIGNATURE -> {
                                                if (savedSignaturePaths.isEmpty()) {
                                                    showSignaturePadSheet = true
                                                } else {
                                                    pushUndoSnapshot()
                                                    val wNorm = signatureScaleFactor.coerceIn(0.15f, 0.65f)
                                                    val hNorm = (wNorm * 0.45f).coerceIn(0.08f, 0.35f)
                                                    val stamp = PdfAnnotationItem.SignatureStamp(
                                                        xNorm = (nx - wNorm / 2f).coerceIn(0.02f, 1f - wNorm),
                                                        yNorm = (ny - hNorm / 2f).coerceIn(0.02f, 1f - hNorm),
                                                        widthNorm = wNorm,
                                                        heightNorm = hNorm,
                                                        pathsNormalized = savedSignaturePaths.toList(),
                                                        colorArgb = selectedColor.toArgb()
                                                    )
                                                    val p = pages[safePageIndex]
                                                    pages[safePageIndex] = p.copy(annotations = p.annotations + stamp)
                                                    autoSaveDraft()
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                } else if (activeTool == PdfAnnotationTool.PEN ||
                                    activeTool == PdfAnnotationTool.HIGHLIGHTER ||
                                    activeTool == PdfAnnotationTool.REDACT ||
                                    activeTool == PdfAnnotationTool.RECTANGLE ||
                                    activeTool == PdfAnnotationTool.ARROW
                                ) {
                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            val nx = (startOffset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                            val ny = (startOffset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                            if (activeTool == PdfAnnotationTool.PEN || activeTool == PdfAnnotationTool.HIGHLIGHTER) {
                                                currentStrokePoints.clear()
                                                currentStrokePoints.add(PointF(nx, ny))
                                            } else {
                                                shapeDragStart = PointF(nx, ny)
                                                shapeDragEnd = PointF(nx, ny)
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val nx = (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                            val ny = (change.position.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                            if (activeTool == PdfAnnotationTool.PEN || activeTool == PdfAnnotationTool.HIGHLIGHTER) {
                                                currentStrokePoints.add(PointF(nx, ny))
                                            } else {
                                                shapeDragEnd = PointF(nx, ny)
                                            }
                                        },
                                        onDragEnd = {
                                            if (activeTool == PdfAnnotationTool.PEN || activeTool == PdfAnnotationTool.HIGHLIGHTER) {
                                                if (currentStrokePoints.size >= 2) {
                                                    pushUndoSnapshot()
                                                    val isHi = activeTool == PdfAnnotationTool.HIGHLIGHTER
                                                    val stroke = PdfAnnotationItem.FreehandStroke(
                                                        pointsNormalized = currentStrokePoints.toList(),
                                                        colorArgb = selectedColor.toArgb(),
                                                        strokeWidthRatio = if (isHi) 0.028f else 0.006f,
                                                        isHighlighter = isHi
                                                    )
                                                    val p = pages[safePageIndex]
                                                    pages[safePageIndex] = p.copy(annotations = p.annotations + stroke)
                                                    autoSaveDraft()
                                                }
                                                currentStrokePoints.clear()
                                            } else if (activeTool == PdfAnnotationTool.REDACT) {
                                                val s = shapeDragStart
                                                val e = shapeDragEnd
                                                if (s != null && e != null) {
                                                    pushUndoSnapshot()
                                                    val p = pages[safePageIndex]
                                                    // Permanently burn black box into baseBitmap so underlying pixels are destroyed
                                                    val flattenedBmp = DocumentSmartTools.renderPageWithPdfPackTools(
                                                        source = rotateBitmapIfNeeded(p.baseBitmap, p.rotationDegrees),
                                                        pageIndex = safePageIndex,
                                                        totalPages = pages.size,
                                                        redactionBoxesNormalized = listOf(s to e)
                                                    )
                                                    pages[safePageIndex] = p.copy(
                                                        baseBitmap = flattenedBmp,
                                                        rotationDegrees = 0
                                                    )
                                                    autoSaveDraft()
                                                    statusBanner = "Redacted area flattened permanently into page ${safePageIndex + 1}"
                                                }
                                                shapeDragStart = null
                                                shapeDragEnd = null
                                            } else {
                                                val s = shapeDragStart
                                                val e = shapeDragEnd
                                                if (s != null && e != null) {
                                                    pushUndoSnapshot()
                                                    val shape = PdfAnnotationItem.ShapeItem(
                                                        type = activeTool,
                                                        startNorm = s,
                                                        endNorm = e,
                                                        colorArgb = selectedColor.toArgb()
                                                    )
                                                    val p = pages[safePageIndex]
                                                    pages[safePageIndex] = p.copy(annotations = p.annotations + shape)
                                                    autoSaveDraft()
                                                }
                                                shapeDragStart = null
                                                shapeDragEnd = null
                                            }
                                        }
                                    )
                                }
                            }
                    ) {
                        val cw = size.width
                        val ch = size.height

                        // Draw committed annotations
                        currentPage.annotations.forEach { item ->
                            drawAnnotationOnComposeCanvas(item, cw, ch)
                        }

                        // Draw live in-progress stroke
                        if (currentStrokePoints.size >= 2) {
                            val isHi = activeTool == PdfAnnotationTool.HIGHLIGHTER
                            val path = Path().apply {
                                val first = currentStrokePoints.first()
                                moveTo(first.x * cw, first.y * ch)
                                for (i in 1 until currentStrokePoints.size) {
                                    val pt = currentStrokePoints[i]
                                    lineTo(pt.x * cw, pt.y * ch)
                                }
                            }
                            drawPath(
                                path = path,
                                color = if (isHi) selectedColor.copy(alpha = 0.38f) else selectedColor,
                                style = Stroke(
                                    width = cw * (if (isHi) 0.028f else 0.006f),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Draw live in-progress shape or redaction box
                        val s = shapeDragStart
                        val e = shapeDragEnd
                        if (s != null && e != null) {
                            if (activeTool == PdfAnnotationTool.REDACT) {
                                val left = min(s.x, e.x) * cw
                                val top = min(s.y, e.y) * ch
                                val rw = max(12f, kotlin.math.abs(e.x - s.x) * cw)
                                val rh = max(12f, kotlin.math.abs(e.y - s.y) * ch)
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(left, top),
                                    size = Size(rw, rh)
                                )
                            } else {
                                drawAnnotationOnComposeCanvas(
                                    PdfAnnotationItem.ShapeItem(
                                        type = activeTool,
                                        startNorm = s,
                                        endNorm = e,
                                        colorArgb = selectedColor.toArgb()
                                    ),
                                    cw,
                                    ch
                                )
                            }
                        }
                    }
                }

                // Page Indicator Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Page ${safePageIndex + 1} / ${pages.size}",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                }
            }
        }

        // PageThumbnail Strip
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("pdf_page_thumbnail_strip")
        ) {
            pages.forEachIndexed { idx, page ->
                val isSelected = idx == safePageIndex
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) theme.accent else Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .pressScaleClickable(
                            testTag = "pdf_thumb_${idx + 1}",
                            onClick = { selectedPageIndex = idx }
                        )
                ) {
                    Image(
                        bitmap = page.baseBitmap.asImageBitmap(),
                        contentDescription = "Thumbnail Page ${idx + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.68f))
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) theme.accent else Color.White
                        )
                    }
                }
            }
        }

        // Page Organization Controls: Rotate, Move Left/Right, Duplicate, Delete, Extract, Split
        GlassSurface(
            hazeState = hazeState,
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PAGE ORGANIZATION (PAGE ${safePageIndex + 1})",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Rotate 90°",
                        icon = Icons.Default.RotateRight,
                        onClick = {
                            if (currentPage != null) {
                                pushUndoSnapshot()
                                pages[safePageIndex] = currentPage.copy(
                                    rotationDegrees = (currentPage.rotationDegrees + 90) % 360
                                )
                                autoSaveDraft()
                            }
                        },
                        testTag = "pdf_rotate_page_button"
                    )

                    GlassButton(
                        text = "Move ◀",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = {
                            if (safePageIndex > 0) {
                                pushUndoSnapshot()
                                val item = pages.removeAt(safePageIndex)
                                pages.add(safePageIndex - 1, item)
                                selectedPageIndex = safePageIndex - 1
                                autoSaveDraft()
                            }
                        },
                        testTag = "pdf_move_left_button"
                    )

                    GlassButton(
                        text = "Move ▶",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = {
                            if (safePageIndex < pages.lastIndex) {
                                pushUndoSnapshot()
                                val item = pages.removeAt(safePageIndex)
                                pages.add(safePageIndex + 1, item)
                                selectedPageIndex = safePageIndex + 1
                                autoSaveDraft()
                            }
                        },
                        testTag = "pdf_move_right_button"
                    )

                    GlassButton(
                        text = "Duplicate",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            if (currentPage != null) {
                                pushUndoSnapshot()
                                val copy = currentPage.copy(
                                    id = System.nanoTime(),
                                    annotations = currentPage.annotations.toList()
                                )
                                pages.add(safePageIndex + 1, copy)
                                selectedPageIndex = safePageIndex + 1
                                autoSaveDraft()
                                statusBanner = "Duplicated page ${safePageIndex}"
                            }
                        },
                        testTag = "pdf_duplicate_page_button"
                    )

                    GlassButton(
                        text = "Delete Page",
                        icon = Icons.Default.Delete,
                        onClick = {
                            if (pages.size > 1) {
                                pushUndoSnapshot()
                                pages.removeAt(safePageIndex)
                                selectedPageIndex = safePageIndex.coerceAtMost(pages.lastIndex)
                                autoSaveDraft()
                                statusBanner = "Deleted page ${safePageIndex + 1}"
                            } else {
                                statusBanner = "A PDF must keep at least 1 page."
                            }
                        },
                        testTag = "pdf_delete_page_button"
                    )

                    GlassButton(
                        text = "Extract Page",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            if (currentPage != null) {
                                val bmp = renderEditablePageToBitmap(currentPage)
                                scanViewModel.createAndSaveDocument(
                                    context = context,
                                    name = "${documentTitle.ifBlank { "PDF" }} - Page ${safePageIndex + 1}",
                                    category = "Other",
                                    filterMode = "Original Color",
                                    pageBitmaps = listOf(bmp),
                                    pageSize = selectedPageSize,
                                    fitMode = selectedFitMode,
                                    marginMode = selectedMarginMode,
                                    qualityDpi = selectedQualityDpi
                                ) { savedDoc ->
                                    statusBanner = "Extracted Page ${safePageIndex + 1} to Library (\"${savedDoc.name}\")"
                                }
                            }
                        },
                        testTag = "pdf_extract_page_button"
                    )

                    GlassButton(
                        text = "Split Here",
                        icon = Icons.Default.CallSplit,
                        onClick = {
                            if (pages.size < 2) {
                                statusBanner = "Need at least 2 pages to split a PDF."
                            } else {
                                val splitIndex = if (safePageIndex == 0) 1 else safePageIndex
                                val part1Bitmaps = pages.subList(0, splitIndex).map { renderEditablePageToBitmap(it) }
                                val part2Bitmaps = pages.subList(splitIndex, pages.size).map { renderEditablePageToBitmap(it) }
                                val baseTitle = documentTitle.ifBlank { "Split PDF" }
                                scanViewModel.createAndSaveDocument(
                                    context = context,
                                    name = "$baseTitle (Part 1)",
                                    category = "Other",
                                    filterMode = "Original Color",
                                    pageBitmaps = part1Bitmaps,
                                    pageSize = selectedPageSize
                                )
                                scanViewModel.createAndSaveDocument(
                                    context = context,
                                    name = "$baseTitle (Part 2)",
                                    category = "Other",
                                    filterMode = "Original Color",
                                    pageBitmaps = part2Bitmaps,
                                    pageSize = selectedPageSize
                                ) {
                                    statusBanner = "Split into Part 1 (${part1Bitmaps.size}p) & Part 2 (${part2Bitmaps.size}p) in Library"
                                }
                            }
                        },
                        testTag = "pdf_split_button"
                    )
                }
            }
        }

        // Annotation & Signature Toolbar
        GlassSurface(
            hazeState = hazeState,
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ANNOTATE & SIGN (${activeTool.label.uppercase()})",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.pressScaleClickable(
                            testTag = "pdf_open_signature_pad_button",
                            onClick = { showSignaturePadSheet = true }
                        )
                    ) {
                        Text(
                            text = "Signatures (${savedSignatureLibrary.size}/5)",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.bentoCoral
                        )
                        ProBadge()
                    }
                }

                // Tool Selector Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    PdfAnnotationTool.entries.forEach { tool ->
                        val isSelected = activeTool == tool
                        val icon = when (tool) {
                            PdfAnnotationTool.VIEW_PAN -> Icons.Default.PanTool
                            PdfAnnotationTool.PEN -> Icons.Default.Edit
                            PdfAnnotationTool.HIGHLIGHTER -> Icons.Default.Highlight
                            PdfAnnotationTool.TEXT_BOX -> Icons.Default.TextFields
                            PdfAnnotationTool.REDACT -> Icons.Default.VisibilityOff
                            PdfAnnotationTool.RECTANGLE -> Icons.Default.CropSquare
                            PdfAnnotationTool.ARROW -> Icons.Default.SouthEast
                            PdfAnnotationTool.CHECKMARK -> Icons.Default.Check
                            PdfAnnotationTool.SIGNATURE -> Icons.Default.Draw
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) theme.accent else theme.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) theme.accent else Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .pressScaleClickable(
                                    testTag = "pdf_tool_${tool.name.lowercase()}",
                                    onClick = {
                                        if (tool == PdfAnnotationTool.REDACT) {
                                            requirePdfPackFeature(DocumentPackFeature.PDF_REDACT) {
                                                activeTool = PdfAnnotationTool.REDACT
                                                statusBanner = "Redact active: drag a black box over sensitive text to permanently destroy underlying pixels."
                                            }
                                        } else {
                                            activeTool = tool
                                            if (tool == PdfAnnotationTool.SIGNATURE && savedSignaturePaths.isEmpty()) {
                                                showSignaturePadSheet = true
                                            }
                                        }
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tool.label,
                                    tint = if (isSelected) BentoTextBlack else theme.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tool.label,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) BentoTextBlack else theme.textPrimary
                                )
                                if (tool.isProTool) {
                                    ProBadge()
                                }
                            }
                        }
                    }
                }

                // Color Swatches Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "COLOR",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    colorChoices.forEachIndexed { idx, swatch ->
                        val isSelected = selectedColor == swatch
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .pressScaleClickable(
                                    testTag = "pdf_color_swatch_$idx",
                                    onClick = { selectedColor = swatch }
                                )
                        )
                    }
                }

                // Text Box Font, Size & Form-Fill Snippets (Name, Address, Date) when Text Box is selected
                if (activeTool == PdfAnnotationTool.TEXT_BOX) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PdfFontChoice.entries.forEach { fontChoice ->
                                val isSelected = selectedFont == fontChoice
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) theme.accent else theme.surface)
                                        .pressScaleClickable { selectedFont = fontChoice }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = fontChoice.label,
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) BentoTextBlack else theme.textPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Font Size",
                                tint = theme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Slider(
                                value = selectedFontSizeSp,
                                onValueChange = { selectedFontSizeSp = it },
                                valueRange = 12f..36f,
                                colors = SliderDefaults.colors(
                                    thumbColor = theme.accent,
                                    activeTrackColor = theme.accent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${selectedFontSizeSp.roundToInt()}pt",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                        }

                        // Form-Fill Snippets Row (Name, Address, Date)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "SNIPPETS:",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.textSecondary
                            )
                            ProBadge()
                            listOf(
                                "Name" to formSnippets.name,
                                "Address" to formSnippets.address,
                                "Date" to formSnippets.date
                            ).forEach { (label, snippetValue) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(theme.surface)
                                        .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .pressScaleClickable(
                                            testTag = "pdf_snippet_${label.lowercase()}",
                                            onClick = {
                                                requirePdfPackFeature(DocumentPackFeature.PDF_FORM_SNIPPETS) {
                                                    if (currentPage != null) {
                                                        pushUndoSnapshot()
                                                        val item = PdfAnnotationItem.TextBoxItem(
                                                            text = snippetValue,
                                                            xNorm = 0.14f,
                                                            yNorm = when (label) {
                                                                "Name" -> 0.24f
                                                                "Address" -> 0.30f
                                                                else -> 0.36f
                                                            },
                                                            fontChoice = selectedFont,
                                                            fontSizeSp = selectedFontSizeSp,
                                                            colorArgb = selectedColor.toArgb()
                                                        )
                                                        pages[safePageIndex] = currentPage.copy(
                                                            annotations = currentPage.annotations + item
                                                        )
                                                        autoSaveDraft()
                                                        statusBanner = "Placed $label snippet (\"$snippetValue\") on page ${safePageIndex + 1}"
                                                    }
                                                }
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "+ $label",
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = theme.accent
                                    )
                                }
                            }
                            Text(
                                text = if (showEditSnippetsRow) "Done" else "Edit",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.bentoCoral,
                                modifier = Modifier
                                    .pressScaleClickable {
                                        requirePdfPackFeature(DocumentPackFeature.PDF_FORM_SNIPPETS) {
                                            showEditSnippetsRow = !showEditSnippetsRow
                                        }
                                    }
                                    .padding(horizontal = 6.dp)
                            )
                        }

                        if (showEditSnippetsRow) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = formSnippets.name,
                                    onValueChange = {
                                        formSnippets = formSnippets.copy(name = it)
                                        DocumentSmartTools.saveFormFillSnippets(context, formSnippets)
                                    },
                                    label = { Text("Saved Name Snippet") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = formSnippets.address,
                                    onValueChange = {
                                        formSnippets = formSnippets.copy(address = it)
                                        DocumentSmartTools.saveFormFillSnippets(context, formSnippets)
                                    },
                                    label = { Text("Saved Address Snippet") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = formSnippets.date,
                                    onValueChange = {
                                        formSnippets = formSnippets.copy(date = it)
                                        DocumentSmartTools.saveFormFillSnippets(context, formSnippets)
                                    },
                                    label = { Text("Saved Date Snippet") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Signature Size Slider & Saved Signatures Picker when Signature is selected
                if (activeTool == PdfAnnotationTool.SIGNATURE) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (savedSignatureLibrary.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Saved (${savedSignatureLibrary.size}/5):",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = theme.textSecondary
                                )
                                savedSignatureLibrary.forEachIndexed { idx, sigItem ->
                                    val isCurrent = savedSignaturePaths.toList() == sigItem.pathsNormalized
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCurrent) theme.accent else theme.surface)
                                            .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .pressScaleClickable(
                                                testTag = "pdf_saved_sig_chip_$idx",
                                                onClick = {
                                                    if (idx > 0) {
                                                        requirePdfPackFeature(DocumentPackFeature.PDF_SAVED_SIGNATURES) {
                                                            savedSignaturePaths.clear()
                                                            savedSignaturePaths.addAll(sigItem.pathsNormalized)
                                                            statusBanner = "Selected \"${sigItem.label}\" — tap page to place"
                                                        }
                                                    } else {
                                                        savedSignaturePaths.clear()
                                                        savedSignaturePaths.addAll(sigItem.pathsNormalized)
                                                        statusBanner = "Selected \"${sigItem.label}\" — tap page to place"
                                                    }
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = sigItem.label,
                                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                                            color = if (isCurrent) BentoTextBlack else theme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Signature Size (Tap page to place):",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                            Slider(
                                value = signatureScaleFactor,
                                onValueChange = { signatureScaleFactor = it },
                                valueRange = 0.16f..0.55f,
                                colors = SliderDefaults.colors(
                                    thumbColor = theme.accent,
                                    activeTrackColor = theme.accent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // PREMIUM PDF PACK TOOLS CARD (Compress, Password, Watermark, Page Numbers, Header/Footer, Stamps, Crop, Redact)
        GlassSurface(
            hazeState = hazeState,
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pdf_premium_pack_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PREMIUM PDF PACK TOOLS",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                        ProBadge()
                    }
                    if (pdfPasswordToApply.isNotBlank()) {
                        Text(
                            text = "🔒 Encrypted on Export",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.bentoCoral
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProPdfToolChip(
                        label = "Compress",
                        icon = Icons.Default.Compress,
                        isActive = false,
                        testTag = "pdf_pro_compress_button",
                        onClick = {
                            requirePdfPackFeature(DocumentPackFeature.PDF_COMPRESS) {
                                showCompressSheet = true
                            }
                        }
                    )
                    ProPdfToolChip(
                        label = if (pdfPasswordToApply.isNotBlank()) "Password ✓" else "Password",
                        icon = if (pdfPasswordToApply.isNotBlank()) Icons.Default.Lock else Icons.Default.LockOpen,
                        isActive = pdfPasswordToApply.isNotBlank(),
                        testTag = "pdf_pro_password_button",
                        onClick = {
                            requirePdfPackFeature(DocumentPackFeature.PDF_PASSWORD) {
                                showPasswordSheet = true
                            }
                        }
                    )
                    ProPdfToolChip(
                        label = if (watermarkConfig.text.isNotBlank() || watermarkImageBitmap != null) "Watermark ✓" else "Watermark",
                        icon = Icons.Default.WaterDrop,
                        isActive = watermarkConfig.text.isNotBlank() || watermarkImageBitmap != null,
                        testTag = "pdf_pro_watermark_button",
                        onClick = {
                            requirePdfPackFeature(DocumentPackFeature.PDF_WATERMARK) {
                                showWatermarkSheet = true
                            }
                        }
                    )
                    ProPdfToolChip(
                        label = if (pageNumberConfig.enabled) "Page # ✓" else "Page Numbers",
                        icon = Icons.Default.FormatListNumbered,
                        isActive = pageNumberConfig.enabled,
                        testTag = "pdf_pro_page_numbers_button",
                        onClick = {
                            requirePdfPackFeature(DocumentPackFeature.PDF_PAGE_NUMBERS) {
                                showPageNumbersHeaderSheet = true
                            }
                        }
                    )
                    ProPdfToolChip(
                        label = if (headerFooterConfig.headerText.isNotBlank() || headerFooterConfig.footerText.isNotBlank()) "Header/Footer ✓" else "Header & Footer",
                        icon = Icons.Default.TextFields,
                        isActive = headerFooterConfig.headerText.isNotBlank() || headerFooterConfig.footerText.isNotBlank(),
                        testTag = "pdf_pro_header_footer_button",
                        onClick = {
                            requirePdfPackFeature(DocumentPackFeature.PDF_HEADER_FOOTER) {
                                showPageNumbersHeaderSheet = true
                            }
                        }
                    )
                    ProPdfToolChip(
                        label = "Stamps",
                        icon = Icons.Default.Verified,
                        isActive = activeStampToPlace != null,
                        testTag = "pdf_pro_stamps_button",
                        onClick = {
                            requirePdfPackFeature(DocumentPackFeature.PDF_STAMPS) {
                                showStampsSheet = true
                            }
                        }
                    )
                    ProPdfToolChip(
                        label = "Crop Pages",
                        icon = Icons.Default.Crop,
                        isActive = false,
                        testTag = "pdf_pro_crop_button",
                        onClick = {
                            requirePdfPackFeature(DocumentPackFeature.PDF_CROP) {
                                showCropSheet = true
                            }
                        }
                    )
                    ProPdfToolChip(
                        label = "Redact",
                        icon = Icons.Default.VisibilityOff,
                        isActive = activeTool == PdfAnnotationTool.REDACT,
                        testTag = "pdf_pro_redact_button",
                        onClick = {
                            requirePdfPackFeature(DocumentPackFeature.PDF_REDACT) {
                                activeTool = PdfAnnotationTool.REDACT
                                statusBanner = "Redact mode: drag a box on the page to permanently black out & flatten hidden content."
                            }
                        }
                    )
                }
            }
        }

        // Helper to render all pages including Watermark, Header/Footer, Page Numbers, and Stamps
        fun renderAllFinalBitmaps(): List<Bitmap> {
            return pages.mapIndexed { idx, p ->
                val baseAnnotated = renderEditablePageToBitmap(p)
                DocumentSmartTools.renderPageWithPdfPackTools(
                    source = baseAnnotated,
                    pageIndex = idx,
                    totalPages = pages.size,
                    watermark = watermarkConfig.takeIf { it.text.isNotBlank() || watermarkImageBitmap != null },
                    watermarkImage = watermarkImageBitmap,
                    pageNumber = pageNumberConfig.takeIf { it.enabled },
                    headerFooter = headerFooterConfig.takeIf { it.headerText.isNotBlank() || it.footerText.isNotBlank() }
                )
            }
        }

        // Daily PDF Export Allowance Chip + Bottom Export / Share / Save to Phone Row
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "PDF Export & Share",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )
            QuotaAllowanceChip(
                quotaType = QuotaType.PDF_EXPORT,
                isProUnlocked = isUnlimitedMergeUnlocked,
                onClick = { featureGate?.openQuotaUnlockSheet(QuotaType.PDF_EXPORT) }
            )
        }

        fun requirePdfExportQuota(onAllowed: () -> Unit) {
            if (isUnlimitedMergeUnlocked || featureGate == null) {
                onAllowed()
            } else {
                featureGate.requireQuotaOrUnlock(
                    quotaType = QuotaType.PDF_EXPORT,
                    onAllowed = onAllowed
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Save to Library",
                icon = Icons.Default.Save,
                accentFill = theme.accent,
                onClick = {
                    requirePdfExportQuota {
                        val renderedBitmaps = renderAllFinalBitmaps()
                        scanViewModel.createAndSaveDocument(
                            context = context,
                            name = documentTitle.ifBlank { ScanRepository.defaultDocumentName() },
                            category = "Other",
                            filterMode = "Original Color",
                            pageBitmaps = renderedBitmaps,
                            pageSize = selectedPageSize,
                            fitMode = selectedFitMode,
                            marginMode = selectedMarginMode,
                            qualityDpi = selectedQualityDpi
                        ) { savedDoc ->
                            if (pdfPasswordToApply.isNotBlank()) {
                                scanViewModel.protectDocumentWithPassword(context, savedDoc, pdfPasswordToApply)
                            }
                            FeedbackHelper.vibrateTick(context, 35L, 200)
                            statusBanner = "Saved \"${savedDoc.name}\" (${savedDoc.pageCount} pages) to Library!"
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "pdf_save_to_library_button"
            )

            GlassButton(
                text = "Share",
                icon = Icons.Default.Share,
                onClick = {
                    requirePdfExportQuota {
                        val renderedBitmaps = renderAllFinalBitmaps()
                        scanViewModel.createAndSaveDocument(
                            context = context,
                            name = documentTitle.ifBlank { ScanRepository.defaultDocumentName() },
                            category = "Other",
                            filterMode = "Original Color",
                            pageBitmaps = renderedBitmaps,
                            pageSize = selectedPageSize,
                            fitMode = selectedFitMode,
                            marginMode = selectedMarginMode,
                            qualityDpi = selectedQualityDpi
                        ) { savedDoc ->
                            if (pdfPasswordToApply.isNotBlank()) {
                                scanViewModel.protectDocumentWithPassword(context, savedDoc, pdfPasswordToApply)
                            }
                            scanViewModel.shareDocumentPdf(context, savedDoc) { chooser ->
                                context.startActivity(chooser)
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "pdf_share_button"
            )

            GlassButton(
                text = "Phone",
                icon = Icons.Default.Download,
                onClick = {
                    requirePdfExportQuota {
                        val renderedBitmaps = renderAllFinalBitmaps()
                        scanViewModel.createAndSaveDocument(
                            context = context,
                            name = documentTitle.ifBlank { ScanRepository.defaultDocumentName() },
                            category = "Other",
                            filterMode = "Original Color",
                            pageBitmaps = renderedBitmaps,
                            pageSize = selectedPageSize,
                            fitMode = selectedFitMode,
                            marginMode = selectedMarginMode,
                            qualityDpi = selectedQualityDpi
                        ) { savedDoc ->
                            if (pdfPasswordToApply.isNotBlank()) {
                                scanViewModel.protectDocumentWithPassword(context, savedDoc, pdfPasswordToApply)
                            }
                            scanViewModel.saveDocumentToPhone(context, savedDoc)
                            statusBanner = "Saved \"${savedDoc.name}.pdf\" to Downloads & Library"
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "pdf_save_to_phone_button"
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // Text Box Input Sheet (with Form-Fill Snippets: Name, Address, Date)
    if (pendingTextPlacementNorm != null) {
        val placement = pendingTextPlacementNorm!!
        GlassSheet(
            onDismissRequest = { pendingTextPlacementNorm = null },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add Text Box / Form Snippet",
                    style = type.title,
                    color = theme.textPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Form-Fill:", style = type.caption, color = theme.textSecondary)
                    ProBadge()
                    listOf(
                        "Name" to formSnippets.name,
                        "Address" to formSnippets.address,
                        "Date" to formSnippets.date
                    ).forEach { (label, valStr) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.surface)
                                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .pressScaleClickable {
                                    requirePdfPackFeature(DocumentPackFeature.PDF_FORM_SNIPPETS) {
                                        textBoxInput = valStr
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(label, style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                        }
                    }
                }
                OutlinedTextField(
                    value = textBoxInput,
                    onValueChange = { textBoxInput = it },
                    label = { Text("Annotation Text") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = { pendingTextPlacementNorm = null },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Place Text",
                        icon = Icons.Default.Check,
                        accentFill = theme.accent,
                        onClick = {
                            if (textBoxInput.isNotBlank() && currentPage != null) {
                                pushUndoSnapshot()
                                val item = PdfAnnotationItem.TextBoxItem(
                                    text = textBoxInput.trim(),
                                    xNorm = placement.x,
                                    yNorm = placement.y,
                                    fontChoice = selectedFont,
                                    fontSizeSp = selectedFontSizeSp,
                                    colorArgb = selectedColor.toArgb()
                                )
                                pages[safePageIndex] = currentPage.copy(
                                    annotations = currentPage.annotations + item
                                )
                                autoSaveDraft()
                            }
                            pendingTextPlacementNorm = null
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pdf_confirm_text_box_button"
                    )
                }
            }
        }
    }

    // Signature & Initials Drawing Sheet (Up to 5 saved signatures and initials)
    if (showSignaturePadSheet) {
        SignaturePadGlassSheet(
            hazeState = hazeState,
            savedSignatures = savedSignatureLibrary,
            onSelectSaved = { item ->
                savedSignaturePaths.clear()
                savedSignaturePaths.addAll(item.pathsNormalized)
                showSignaturePadSheet = false
                activeTool = PdfAnnotationTool.SIGNATURE
                statusBanner = "Selected \"${item.label}\" — tap anywhere on the page to place it."
            },
            onDeleteSaved = { id ->
                val updated = DocumentSmartTools.deleteSavedSignature(context, id)
                savedSignatureLibrary.clear()
                savedSignatureLibrary.addAll(updated)
            },
            onSaveSignature = { label, isInitials, drawnPaths ->
                if (savedSignatureLibrary.isNotEmpty()) {
                    requirePdfPackFeature(DocumentPackFeature.PDF_SAVED_SIGNATURES) {
                        val updated = DocumentSmartTools.saveSignatureItem(context, label, isInitials, drawnPaths)
                        savedSignatureLibrary.clear()
                        savedSignatureLibrary.addAll(updated)
                        savedSignaturePaths.clear()
                        savedSignaturePaths.addAll(drawnPaths)
                        showSignaturePadSheet = false
                        activeTool = PdfAnnotationTool.SIGNATURE
                        statusBanner = "Saved \"$label\" (${savedSignatureLibrary.size}/5)! Tap page to place."
                    }
                } else {
                    val updated = DocumentSmartTools.saveSignatureItem(context, label, isInitials, drawnPaths)
                    savedSignatureLibrary.clear()
                    savedSignatureLibrary.addAll(updated)
                    savedSignaturePaths.clear()
                    savedSignaturePaths.addAll(drawnPaths)
                    showSignaturePadSheet = false
                    activeTool = PdfAnnotationTool.SIGNATURE
                    statusBanner = "Signature saved! Tap anywhere on the page to place it."
                }
            },
            onDismiss = { showSignaturePadSheet = false }
        )
    }

    // 1. Compress PDF Sheet (Low / Medium / High with before & after size)
    if (showCompressSheet) {
        var selectedLevel by remember { mutableStateOf(PdfCompressionLevel.MEDIUM) }
        var compressResult by remember { mutableStateOf<PdfCompressResult?>(null) }
        var isCompressing by remember { mutableStateOf(false) }

        LaunchedEffect(selectedLevel, pages.size) {
            isCompressing = true
            val bitmaps = pages.map { renderEditablePageToBitmap(it) }
            val outFile = File(context.cacheDir, "compressed_preview_${selectedLevel.id}.pdf")
            compressResult = DocumentSmartTools.compressBitmapsToPdf(
                context = context,
                pageBitmaps = bitmaps,
                pageSize = selectedPageSize,
                level = selectedLevel,
                outputFile = outFile
            )
            isCompressing = false
        }

        GlassSheet(
            onDismissRequest = { showCompressSheet = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Compress PDF", style = type.title, color = theme.textPrimary)
                    ProBadge()
                }
                Text(
                    text = "Choose compression strength. View real before and after PDF sizes before saving.",
                    style = type.caption,
                    color = theme.textSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PdfCompressionLevel.entries.forEach { level ->
                        val selected = selectedLevel == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) theme.accent else theme.surface)
                                .border(1.dp, if (selected) theme.accent else Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                .pressScaleClickable(
                                    testTag = "pdf_compress_level_${level.id}",
                                    onClick = { selectedLevel = level }
                                )
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = level.label,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) BentoTextBlack else theme.textPrimary
                                )
                                Text(
                                    text = level.subtitle,
                                    style = type.caption.copy(fontSize = 10.sp),
                                    color = if (selected) BentoTextBlack.copy(alpha = 0.75f) else theme.textSecondary
                                )
                            }
                        }
                    }
                }

                val res = compressResult
                if (res != null && !isCompressing) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.surface)
                            .border(1.dp, theme.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                            .testTag("pdf_compress_before_after_card")
                    ) {
                        Column {
                            Text("Before Size", style = type.caption, color = theme.textSecondary)
                            Text(
                                text = ScanRepository.formatFileSize(res.beforeBytes),
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                        }
                        Text("→", style = type.title, color = theme.accent)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("After Size (-${res.savedPercent}%)", style = type.caption, color = theme.accent)
                            Text(
                                text = ScanRepository.formatFileSize(res.afterBytes),
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.accent
                            )
                        }
                    }
                } else {
                    Text("Calculating before & after size...", style = type.caption, color = theme.accent)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = { showCompressSheet = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Save Compressed",
                        icon = Icons.Default.Save,
                        accentFill = theme.accent,
                        onClick = {
                            val r = compressResult
                            val quality = when (selectedLevel) {
                                PdfCompressionLevel.LOW -> PdfQualityDpi.MEDIUM_150
                                PdfCompressionLevel.MEDIUM -> PdfQualityDpi.SMALL_100
                                PdfCompressionLevel.HIGH -> PdfQualityDpi.SMALL_100
                            }
                            selectedQualityDpi = quality
                            val bitmaps = pages.map { renderEditablePageToBitmap(it) }
                            scanViewModel.createAndSaveDocument(
                                context = context,
                                name = "${documentTitle.ifBlank { "Document" }} (Compressed)",
                                category = "Other",
                                filterMode = "Original Color",
                                pageBitmaps = bitmaps,
                                pageSize = selectedPageSize,
                                qualityDpi = quality
                            ) { savedDoc ->
                                if (r != null && r.outputFile.exists()) {
                                    runCatching { r.outputFile.copyTo(File(savedDoc.pdfPath), overwrite = true) }
                                }
                                showCompressSheet = false
                                val beforeStr = r?.let { ScanRepository.formatFileSize(it.beforeBytes) } ?: ""
                                val afterStr = r?.let { ScanRepository.formatFileSize(it.afterBytes) } ?: ""
                                statusBanner = "Compressed PDF ($beforeStr → $afterStr) saved to Library"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pdf_compress_save_button"
                    )
                }
            }
        }
    }

    // 2. Password Protect & Unlock PDF Sheet (Apache-2.0 PdfBox-Android)
    if (showPasswordSheet) {
        var passwordInput by remember { mutableStateOf(pdfPasswordToApply) }
        var unlockMode by remember { mutableStateOf(false) }

        val unlockPdfPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null && passwordInput.isNotBlank()) {
                scope.launch {
                    val tempEncrypted = File(context.cacheDir, "locked_${System.currentTimeMillis()}.pdf")
                    val tempUnlocked = File(context.cacheDir, "unlocked_${System.currentTimeMillis()}.pdf")
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempEncrypted).use { out -> input.copyTo(out) }
                        }
                    }
                    val ok = DocumentSmartTools.unlockPasswordProtectedPdf(
                        context = context,
                        sourcePdfFile = tempEncrypted,
                        password = passwordInput,
                        outputPdfFile = tempUnlocked
                    )
                    if (ok && tempUnlocked.exists()) {
                        val unlockedPages = renderPdfUriToPages(context, Uri.fromFile(tempUnlocked), maxPages = 50)
                        if (unlockedPages.isNotEmpty()) {
                            pushUndoSnapshot()
                            pages.clear()
                            pages.addAll(unlockedPages)
                            selectedPageIndex = 0
                            pdfPasswordToApply = ""
                            showPasswordSheet = false
                            statusBanner = "Unlocked password-protected PDF (${unlockedPages.size} pages) with PdfBox-Android"
                        }
                    } else {
                        statusBanner = "Could not unlock PDF — check the password."
                    }
                }
            }
        }

        GlassSheet(
            onDismissRequest = { showPasswordSheet = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PDF Password Security (PdfBox-Android)", style = type.title, color = theme.textPrimary)
                    ProBadge()
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Protect PDF",
                        icon = Icons.Default.Lock,
                        accentFill = if (!unlockMode) theme.accent else null,
                        onClick = { unlockMode = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Unlock PDF",
                        icon = Icons.Default.LockOpen,
                        accentFill = if (unlockMode) theme.accent else null,
                        onClick = { unlockMode = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text(if (unlockMode) "Enter Current PDF Password" else "Set 128-bit PDF Password") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_password_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = if (pdfPasswordToApply.isNotBlank() && !unlockMode) "Clear Password" else "Cancel",
                        onClick = {
                            if (!unlockMode && pdfPasswordToApply.isNotBlank()) {
                                pdfPasswordToApply = ""
                                statusBanner = "Removed export password"
                            }
                            showPasswordSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = if (unlockMode) "Pick & Unlock PDF" else "Apply Password",
                        icon = if (unlockMode) Icons.Default.LockOpen else Icons.Default.Lock,
                        accentFill = theme.accent,
                        onClick = {
                            if (unlockMode) {
                                if (passwordInput.isBlank()) {
                                    statusBanner = "Enter the PDF password first, then pick the locked PDF."
                                } else {
                                    unlockPdfPicker.launch(arrayOf("application/pdf"))
                                }
                            } else {
                                pdfPasswordToApply = passwordInput.trim()
                                showPasswordSheet = false
                                statusBanner = if (pdfPasswordToApply.isNotBlank()) {
                                    "128-bit password set — exported PDF will be encrypted via PdfBox-Android"
                                } else {
                                    "Password cleared"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pdf_password_confirm_button"
                    )
                }
            }
        }
    }

    // 3. Watermark Sheet (Text or Image, Opacity, Position)
    if (showWatermarkSheet) {
        GlassSheet(
            onDismissRequest = { showWatermarkSheet = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PDF Watermark", style = type.title, color = theme.textPrimary)
                    ProBadge()
                }
                OutlinedTextField(
                    value = watermarkConfig.text,
                    onValueChange = { watermarkConfig = watermarkConfig.copy(text = it) },
                    label = { Text("Watermark Text (e.g., CONFIDENTIAL, DRAFT)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_watermark_text_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = if (watermarkImageBitmap != null) "Change Image Watermark" else "Pick Image Watermark",
                        icon = Icons.Default.AddPhotoAlternate,
                        onClick = {
                            watermarkImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (watermarkImageBitmap != null) {
                        GlassButton(
                            text = "Clear Img",
                            onClick = { watermarkImageBitmap = null }
                        )
                    }
                }

                Text(
                    text = "Opacity: ${(watermarkConfig.opacity * 100).roundToInt()}%",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )
                Slider(
                    value = watermarkConfig.opacity,
                    onValueChange = { watermarkConfig = watermarkConfig.copy(opacity = it) },
                    valueRange = 0.08f..0.85f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                )

                Text(
                    text = "Position",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WatermarkPosition.entries.forEach { pos ->
                        val selected = watermarkConfig.position == pos
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) theme.accent else theme.surface)
                                .pressScaleClickable { watermarkConfig = watermarkConfig.copy(position = pos) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = pos.label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Clear",
                        onClick = {
                            watermarkConfig = WatermarkConfig()
                            watermarkImageBitmap = null
                            showWatermarkSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Apply Watermark",
                        icon = Icons.Default.Check,
                        accentFill = theme.accent,
                        onClick = {
                            if (watermarkConfig.text.isNotBlank() || watermarkImageBitmap != null) {
                                pushUndoSnapshot()
                                for (i in pages.indices) {
                                    val p = pages[i]
                                    val stamped = DocumentSmartTools.renderPageWithPdfPackTools(
                                        source = rotateBitmapIfNeeded(p.baseBitmap, p.rotationDegrees),
                                        pageIndex = i,
                                        totalPages = pages.size,
                                        watermark = watermarkConfig,
                                        watermarkImage = watermarkImageBitmap
                                    )
                                    pages[i] = p.copy(baseBitmap = stamped, rotationDegrees = 0)
                                }
                                autoSaveDraft()
                                statusBanner = "Applied watermark (${watermarkConfig.position.label}) across ${pages.size} page(s)"
                            }
                            showWatermarkSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pdf_watermark_apply_button"
                    )
                }
            }
        }
    }

    // 4. Page Numbers + Header & Footer Sheet
    if (showPageNumbersHeaderSheet) {
        GlassSheet(
            onDismissRequest = { showPageNumbersHeaderSheet = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Page Numbers, Header & Footer", style = type.title, color = theme.textPrimary)
                    ProBadge()
                }

                Text("PAGE NUMBERS", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PageNumberFormat.entries.forEach { fmt ->
                        val selected = pageNumberConfig.enabled && pageNumberConfig.format == fmt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) theme.accent else theme.surface)
                                .pressScaleClickable {
                                    pageNumberConfig = pageNumberConfig.copy(enabled = true, format = fmt)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fmt.label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PageNumberPosition.entries.forEach { pos ->
                        val selected = pageNumberConfig.position == pos
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) theme.accent else theme.surface)
                                .pressScaleClickable {
                                    pageNumberConfig = pageNumberConfig.copy(enabled = true, position = pos)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = pos.label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }

                Text("HEADER & FOOTER", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                OutlinedTextField(
                    value = headerFooterConfig.headerText,
                    onValueChange = { headerFooterConfig = headerFooterConfig.copy(headerText = it) },
                    label = { Text("Header Text (Top of every page)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = headerFooterConfig.footerText,
                    onValueChange = { headerFooterConfig = headerFooterConfig.copy(footerText = it) },
                    label = { Text("Footer Text (Bottom of every page)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Clear",
                        onClick = {
                            pageNumberConfig = PageNumberConfig(enabled = false)
                            headerFooterConfig = HeaderFooterConfig()
                            showPageNumbersHeaderSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Apply to All Pages",
                        icon = Icons.Default.Check,
                        accentFill = theme.accent,
                        onClick = {
                            pushUndoSnapshot()
                            for (i in pages.indices) {
                                val p = pages[i]
                                val stamped = DocumentSmartTools.renderPageWithPdfPackTools(
                                    source = rotateBitmapIfNeeded(p.baseBitmap, p.rotationDegrees),
                                    pageIndex = i,
                                    totalPages = pages.size,
                                    pageNumber = pageNumberConfig.takeIf { it.enabled },
                                    headerFooter = headerFooterConfig.takeIf { it.headerText.isNotBlank() || it.footerText.isNotBlank() }
                                )
                                pages[i] = p.copy(baseBitmap = stamped, rotationDegrees = 0)
                            }
                            autoSaveDraft()
                            showPageNumbersHeaderSheet = false
                            statusBanner = "Applied Page Numbers & Header/Footer across ${pages.size} page(s)"
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pdf_apply_page_numbers_header_button"
                    )
                }
            }
        }
    }

    // 5. Official Stamps Sheet (Approved, Paid, Draft, Confidential)
    if (showStampsSheet) {
        GlassSheet(
            onDismissRequest = { showStampsSheet = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Official PDF Stamps", style = type.title, color = theme.textPrimary)
                    ProBadge()
                }
                Text(
                    text = "Tap any official stamp below to stamp Page ${safePageIndex + 1}.",
                    style = type.caption,
                    color = theme.textSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfOfficialStamp.entries.forEach { stamp ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(theme.surface)
                                .border(1.5.dp, Color(stamp.borderColorArgb), RoundedCornerShape(14.dp))
                                .pressScaleClickable(
                                    testTag = "pdf_stamp_option_${stamp.name.lowercase()}",
                                    onClick = {
                                        if (currentPage != null) {
                                            pushUndoSnapshot()
                                            val stamped = DocumentSmartTools.renderPageWithPdfPackTools(
                                                source = rotateBitmapIfNeeded(currentPage.baseBitmap, currentPage.rotationDegrees),
                                                pageIndex = safePageIndex,
                                                totalPages = pages.size,
                                                stamps = listOf(StampPlacement(stamp = stamp, xNorm = 0.72f, yNorm = 0.18f))
                                            )
                                            pages[safePageIndex] = currentPage.copy(baseBitmap = stamped, rotationDegrees = 0)
                                            autoSaveDraft()
                                            showStampsSheet = false
                                            statusBanner = "Stamped [${stamp.label}] on page ${safePageIndex + 1}"
                                        }
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "[ ${stamp.label} ]",
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = Color(stamp.borderColorArgb)
                            )
                            Text(
                                text = "Stamp Page ${safePageIndex + 1}",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // 6. Crop Pages Sheet
    if (showCropSheet) {
        var leftTrim by remember { mutableFloatStateOf(0.05f) }
        var topTrim by remember { mutableFloatStateOf(0.05f) }
        var rightTrim by remember { mutableFloatStateOf(0.95f) }
        var bottomTrim by remember { mutableFloatStateOf(0.95f) }
        var applyAllPages by remember { mutableStateOf(false) }

        GlassSheet(
            onDismissRequest = { showCropSheet = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Crop PDF Page Margins", style = type.title, color = theme.textPrimary)
                    ProBadge()
                }
                Text(
                    text = "Crop Box: L ${(leftTrim * 100).roundToInt()}% • T ${(topTrim * 100).roundToInt()}% • R ${(rightTrim * 100).roundToInt()}% • B ${(bottomTrim * 100).roundToInt()}%",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
                Text("Left / Right Crop", style = type.caption, color = theme.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slider(
                        value = leftTrim,
                        onValueChange = { leftTrim = it.coerceAtMost(rightTrim - 0.2f) },
                        valueRange = 0f..0.4f,
                        modifier = Modifier.weight(1f)
                    )
                    Slider(
                        value = rightTrim,
                        onValueChange = { rightTrim = it.coerceAtLeast(leftTrim + 0.2f) },
                        valueRange = 0.6f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Top / Bottom Crop", style = type.caption, color = theme.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slider(
                        value = topTrim,
                        onValueChange = { topTrim = it.coerceAtMost(bottomTrim - 0.2f) },
                        valueRange = 0f..0.4f,
                        modifier = Modifier.weight(1f)
                    )
                    Slider(
                        value = bottomTrim,
                        onValueChange = { bottomTrim = it.coerceAtLeast(topTrim + 0.2f) },
                        valueRange = 0.6f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = if (!applyAllPages) "Current Page ✓" else "Current Page",
                        accentFill = if (!applyAllPages) theme.accent else null,
                        onClick = { applyAllPages = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = if (applyAllPages) "All ${pages.size} Pages ✓" else "All ${pages.size} Pages",
                        accentFill = if (applyAllPages) theme.accent else null,
                        onClick = { applyAllPages = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = { showCropSheet = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Apply Crop",
                        icon = Icons.Default.Crop,
                        accentFill = theme.accent,
                        onClick = {
                            pushUndoSnapshot()
                            val cropRect = NormalizedCropRect(leftTrim, topTrim, rightTrim, bottomTrim)
                            val indices = if (applyAllPages) pages.indices.toList() else listOf(safePageIndex)
                            for (idx in indices) {
                                val p = pages[idx]
                                val cropped = DocumentSmartTools.renderPageWithPdfPackTools(
                                    source = rotateBitmapIfNeeded(p.baseBitmap, p.rotationDegrees),
                                    pageIndex = idx,
                                    totalPages = pages.size,
                                    cropRect = cropRect
                                )
                                pages[idx] = p.copy(baseBitmap = cropped, rotationDegrees = 0)
                            }
                            autoSaveDraft()
                            showCropSheet = false
                            statusBanner = if (applyAllPages) {
                                "Cropped all ${pages.size} pages"
                            } else {
                                "Cropped page ${safePageIndex + 1}"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pdf_crop_confirm_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProPdfToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) theme.accent else theme.surface)
            .border(
                width = 1.dp,
                color = if (isActive) theme.accent else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp)
            )
            .pressScaleClickable(testTag = testTag, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) BentoTextBlack else theme.accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = if (isActive) BentoTextBlack else theme.textPrimary
            )
            ProBadge()
        }
    }
}

@Composable
private fun SignaturePadGlassSheet(
    hazeState: HazeState?,
    savedSignatures: List<SavedSignatureItem>,
    onSelectSaved: (SavedSignatureItem) -> Unit,
    onDeleteSaved: (String) -> Unit,
    onSaveSignature: (label: String, isInitials: Boolean, paths: List<List<PointF>>) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val strokes = remember { mutableStateListOf<List<PointF>>() }
    val activeStroke = remember { mutableStateListOf<PointF>() }
    var isInitialsMode by remember { mutableStateOf(false) }
    var labelInput by remember { mutableStateOf("") }

    GlassSheet(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Signatures & Initials (${savedSignatures.size}/5)",
                    style = type.title,
                    color = theme.textPrimary
                )
                ProBadge()
            }

            if (savedSignatures.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    savedSignatures.forEach { sig ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.surface)
                                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sig.label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.accent,
                                modifier = Modifier.pressScaleClickable { onSelectSaved(sig) }
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete ${sig.label}",
                                tint = theme.textSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .pressScaleClickable { onDeleteSaved(sig.id) }
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassButton(
                    text = if (!isInitialsMode) "Signature ✓" else "Signature",
                    accentFill = if (!isInitialsMode) theme.accent else null,
                    onClick = { isInitialsMode = false },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = if (isInitialsMode) "Initials ✓" else "Initials",
                    accentFill = if (isInitialsMode) theme.accent else null,
                    onClick = { isInitialsMode = true },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = labelInput,
                onValueChange = { labelInput = it },
                label = { Text(if (isInitialsMode) "Initials Label (e.g., JD Initials)" else "Signature Label (e.g., Formal Sign)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.5.dp, theme.accent, RoundedCornerShape(18.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    activeStroke.clear()
                                    activeStroke.add(
                                        PointF(
                                            (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f),
                                            (offset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                        )
                                    )
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    activeStroke.add(
                                        PointF(
                                            (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f),
                                            (change.position.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                        )
                                    )
                                },
                                onDragEnd = {
                                    if (activeStroke.size >= 2) {
                                        strokes.add(activeStroke.toList())
                                    }
                                    activeStroke.clear()
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val all = strokes + if (activeStroke.size >= 2) listOf(activeStroke.toList()) else emptyList()
                    all.forEach { pts ->
                        val p = Path().apply {
                            moveTo(pts.first().x * w, pts.first().y * h)
                            for (i in 1 until pts.size) {
                                lineTo(pts[i].x * w, pts[i].y * h)
                            }
                        }
                        drawPath(
                            path = p,
                            color = Color(0xFF0F172A),
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassButton(
                    text = "Clear",
                    onClick = {
                        strokes.clear()
                        activeStroke.clear()
                    },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = if (isInitialsMode) "Save Initials" else "Save Signature",
                    icon = Icons.Default.Check,
                    accentFill = theme.accent,
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            val defaultLabel = if (isInitialsMode) {
                                "Initials ${savedSignatures.count { it.isInitials } + 1}"
                            } else {
                                "Signature ${savedSignatures.count { !it.isInitials } + 1}"
                            }
                            onSaveSignature(labelInput.ifBlank { defaultLabel }, isInitialsMode, strokes.toList())
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "pdf_save_signature_confirm_button"
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAnnotationOnComposeCanvas(
    item: PdfAnnotationItem,
    cw: Float,
    ch: Float
) {
    when (item) {
        is PdfAnnotationItem.FreehandStroke -> {
            if (item.pointsNormalized.size < 2) return
            val path = Path().apply {
                val first = item.pointsNormalized.first()
                moveTo(first.x * cw, first.y * ch)
                for (i in 1 until item.pointsNormalized.size) {
                    val pt = item.pointsNormalized[i]
                    lineTo(pt.x * cw, pt.y * ch)
                }
            }
            val baseColor = Color(item.colorArgb)
            drawPath(
                path = path,
                color = if (item.isHighlighter) baseColor.copy(alpha = 0.36f) else baseColor,
                style = Stroke(
                    width = cw * item.strokeWidthRatio,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        is PdfAnnotationItem.ShapeItem -> {
            val c = Color(item.colorArgb)
            val sx = item.startNorm.x * cw
            val sy = item.startNorm.y * ch
            val ex = item.endNorm.x * cw
            val ey = item.endNorm.y * ch
            val strokeW = (cw * item.strokeWidthRatio).coerceAtLeast(3f)

            when (item.type) {
                PdfAnnotationTool.RECTANGLE -> {
                    val left = min(sx, ex)
                    val top = min(sy, ey)
                    val w = max(12f, kotlin.math.abs(ex - sx))
                    val h = max(12f, kotlin.math.abs(ey - sy))
                    drawRect(
                        color = c,
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        style = Stroke(width = strokeW)
                    )
                }
                PdfAnnotationTool.ARROW -> {
                    drawLine(
                        color = c,
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
                    val headLen = 18f
                    val a1 = angle - Math.toRadians(28.0)
                    val a2 = angle + Math.toRadians(28.0)
                    drawLine(
                        color = c,
                        start = Offset(ex, ey),
                        end = Offset((ex - headLen * cos(a1)).toFloat(), (ey - headLen * sin(a1)).toFloat()),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = c,
                        start = Offset(ex, ey),
                        end = Offset((ex - headLen * cos(a2)).toFloat(), (ey - headLen * sin(a2)).toFloat()),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
                PdfAnnotationTool.CHECKMARK -> {
                    val p = Path().apply {
                        moveTo(sx, sy + 10f)
                        lineTo(sx + 10f, sy + 22f)
                        lineTo(sx + 28f, sy - 4f)
                    }
                    drawPath(
                        path = p,
                        color = c,
                        style = Stroke(width = strokeW * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                else -> {}
            }
        }
        is PdfAnnotationItem.TextBoxItem -> {
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    textSize = item.fontSizeSp * (cw / 360f).coerceAtLeast(1f)
                    typeface = Typeface.create(item.fontChoice.typeface, Typeface.BOLD)
                }
                drawText(item.text, item.xNorm * cw, item.yNorm * ch, paint)
            }
        }
        is PdfAnnotationItem.SignatureStamp -> {
            val c = Color(item.colorArgb)
            val left = item.xNorm * cw
            val top = item.yNorm * ch
            val boxW = item.widthNorm * cw
            val boxH = item.heightNorm * ch
            item.pathsNormalized.forEach { strokePts ->
                if (strokePts.size >= 2) {
                    val p = Path().apply {
                        moveTo(left + strokePts.first().x * boxW, top + strokePts.first().y * boxH)
                        for (i in 1 until strokePts.size) {
                            lineTo(left + strokePts[i].x * boxW, top + strokePts[i].y * boxH)
                        }
                    }
                    drawPath(
                        path = p,
                        color = c,
                        style = Stroke(width = (cw * 0.005f).coerceAtLeast(2.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
        is PdfAnnotationItem.OfficialStampAnnotation -> {
            drawContext.canvas.nativeCanvas.apply {
                DocumentSmartTools.drawOfficialStamp(
                    canvas = this,
                    pageWidth = cw,
                    pageHeight = ch,
                    placement = StampPlacement(item.stamp, item.xNorm, item.yNorm, item.scale)
                )
            }
        }
        is PdfAnnotationItem.RedactionBoxItem -> {
            val left = min(item.startNorm.x, item.endNorm.x) * cw
            val top = min(item.startNorm.y, item.endNorm.y) * ch
            val rw = max(12f, kotlin.math.abs(item.endNorm.x - item.startNorm.x) * cw)
            val rh = max(12f, kotlin.math.abs(item.endNorm.y - item.startNorm.y) * ch)
            drawRect(
                color = Color.Black,
                topLeft = Offset(left, top),
                size = Size(rw, rh)
            )
        }
    }
}

private fun rotateBitmapIfNeeded(source: Bitmap, degrees: Int): Bitmap {
    val norm = ((degrees % 360) + 360) % 360
    if (norm == 0) return source
    val matrix = Matrix().apply { postRotate(norm.toFloat()) }
    return runCatching {
        Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }.getOrDefault(source)
}

/**
 * Flattens an [EditablePdfPage] (base bitmap + rotation + annotations + signature) into a high-resolution Bitmap
 * ready for `PdfDocument` page-by-page export.
 */
fun renderEditablePageToBitmap(page: EditablePdfPage): Bitmap {
    val rotated = rotateBitmapIfNeeded(page.baseBitmap, page.rotationDegrees)
    val out = rotated.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = AndroidCanvas(out)
    val w = out.width.toFloat()
    val h = out.height.toFloat()

    page.annotations.forEach { item ->
        when (item) {
            is PdfAnnotationItem.FreehandStroke -> {
                if (item.pointsNormalized.size >= 2) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = item.colorArgb
                        if (item.isHighlighter) alpha = 92
                        style = Paint.Style.STROKE
                        strokeWidth = (w * item.strokeWidthRatio).coerceAtLeast(3f)
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    val path = AndroidPath().apply {
                        moveTo(item.pointsNormalized.first().x * w, item.pointsNormalized.first().y * h)
                        for (i in 1 until item.pointsNormalized.size) {
                            lineTo(item.pointsNormalized[i].x * w, item.pointsNormalized[i].y * h)
                        }
                    }
                    canvas.drawPath(path, paint)
                }
            }
            is PdfAnnotationItem.ShapeItem -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    style = Paint.Style.STROKE
                    strokeWidth = (w * item.strokeWidthRatio).coerceAtLeast(4f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val sx = item.startNorm.x * w
                val sy = item.startNorm.y * h
                val ex = item.endNorm.x * w
                val ey = item.endNorm.y * h
                when (item.type) {
                    PdfAnnotationTool.RECTANGLE -> {
                        canvas.drawRect(
                            RectF(min(sx, ex), min(sy, ey), max(sx, ex), max(sy, ey)),
                            paint
                        )
                    }
                    PdfAnnotationTool.ARROW -> {
                        canvas.drawLine(sx, sy, ex, ey, paint)
                        val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
                        val headLen = (w * 0.035f).coerceAtLeast(18f)
                        val a1 = angle - Math.toRadians(28.0)
                        val a2 = angle + Math.toRadians(28.0)
                        canvas.drawLine(
                            ex,
                            ey,
                            (ex - headLen * cos(a1)).toFloat(),
                            (ey - headLen * sin(a1)).toFloat(),
                            paint
                        )
                        canvas.drawLine(
                            ex,
                            ey,
                            (ex - headLen * cos(a2)).toFloat(),
                            (ey - headLen * sin(a2)).toFloat(),
                            paint
                        )
                    }
                    PdfAnnotationTool.CHECKMARK -> {
                        val path = AndroidPath().apply {
                            moveTo(sx, sy + h * 0.015f)
                            lineTo(sx + w * 0.02f, sy + h * 0.035f)
                            lineTo(sx + w * 0.055f, sy - h * 0.008f)
                        }
                        canvas.drawPath(path, paint)
                    }
                    else -> {}
                }
            }
            is PdfAnnotationItem.TextBoxItem -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    textSize = item.fontSizeSp * (w / 360f).coerceAtLeast(1f)
                    typeface = Typeface.create(item.fontChoice.typeface, Typeface.BOLD)
                }
                canvas.drawText(item.text, item.xNorm * w, item.yNorm * h, paint)
            }
            is PdfAnnotationItem.SignatureStamp -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    style = Paint.Style.STROKE
                    strokeWidth = (w * 0.005f).coerceAtLeast(3f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val left = item.xNorm * w
                val top = item.yNorm * h
                val boxW = item.widthNorm * w
                val boxH = item.heightNorm * h
                item.pathsNormalized.forEach { pts ->
                    if (pts.size >= 2) {
                        val p = AndroidPath().apply {
                            moveTo(left + pts.first().x * boxW, top + pts.first().y * boxH)
                            for (i in 1 until pts.size) {
                                lineTo(left + pts[i].x * boxW, top + pts[i].y * boxH)
                            }
                        }
                        canvas.drawPath(p, paint)
                    }
                }
            }
            is PdfAnnotationItem.OfficialStampAnnotation -> {
                DocumentSmartTools.drawOfficialStamp(
                    canvas = canvas,
                    pageWidth = w,
                    pageHeight = h,
                    placement = StampPlacement(item.stamp, item.xNorm, item.yNorm, item.scale)
                )
            }
            is PdfAnnotationItem.RedactionBoxItem -> {
                val redactPaint = Paint().apply {
                    color = AndroidColor.BLACK
                    style = Paint.Style.FILL
                }
                canvas.drawRect(
                    RectF(
                        min(item.startNorm.x, item.endNorm.x) * w,
                        min(item.startNorm.y, item.endNorm.y) * h,
                        max(item.startNorm.x, item.endNorm.x) * w,
                        max(item.startNorm.y, item.endNorm.y) * h
                    ),
                    redactPaint
                )
            }
        }
    }
    return out
}

/**
 * Renders pages from a user-selected PDF Uri using `android.graphics.pdf.PdfRenderer`.
 */
suspend fun renderPdfUriToPages(
    context: Context,
    uri: Uri,
    maxPages: Int = 25
): List<EditablePdfPage> = withContext(Dispatchers.IO) {
    val result = mutableListOf<EditablePdfPage>()
    runCatching {
        val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        if (!tempFile.exists() || tempFile.length() == 0L) return@runCatching
        val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        try {
            val count = min(renderer.pageCount, maxPages)
            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val scale = 2.0f // ~144 DPI crisp rendering while keeping memory low
                val bmpW = (page.width * scale).roundToInt().coerceIn(300, 1800)
                val bmpH = (page.height * scale).roundToInt().coerceIn(300, 2400)
                val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(bmp)
                canvas.drawColor(AndroidColor.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                result.add(EditablePdfPage(baseBitmap = bmp))
            }
        } finally {
            renderer.close()
            pfd.close()
            tempFile.delete()
        }
    }
    result
}
