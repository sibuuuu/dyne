package com.example.ui.scan

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.Badge
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.DocumentPackFeature
import com.example.billing.LocalFeatureGate
import com.example.data.DocumentSmartTools
import com.example.data.PdfFitMode
import com.example.data.PdfMarginMode
import com.example.data.PdfPageSize
import com.example.data.PdfQualityDpi
import com.example.data.ProScanFilterMode
import com.example.data.ScanContentType
import com.example.data.ScanRecord
import com.example.data.ScanRepository
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassPermissionCard
import com.example.ui.components.GlassSheet
import com.example.ui.components.HazeState
import com.example.ui.components.ProBadge
import com.example.ui.components.SharedTopBar
import com.example.ui.components.pressScaleClickable
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.util.concurrent.Executors
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Full-screen Live QR & Barcode Scanner with CameraX + bundled ML Kit Barcode Scanning,
 * animated glass viewfinder, Torch toggle, System Photo Picker gallery decoder,
 * and contextual Glass Result Sheet.
 */
@Composable
fun QrBarcodeScannerScreen(
    viewModel: ScanViewModel,
    isPro: Boolean,
    hazeState: HazeState?,
    onNavigateToHistory: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()
    val activeResult by viewModel.activeResultRecord.collectAsStateWithLifecycle()
    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanGalleryUri(
                context = context,
                uri = uri,
                isPro = isPro,
                onSuccessFeedback = {
                    FeedbackHelper.vibrateTick(context, 35L, 200)
                    FeedbackHelper.playBeep(60)
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        SharedTopBar(
            title = "QR & Barcode",
            subtitle = "OFFLINE ML KIT SCANNER",
            hazeState = hazeState,
            onBackClick = onClose,
            rightActionIcon = Icons.Default.History,
            rightActionDescription = "Scan History",
            onRightActionClick = onNavigateToHistory
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (statusToast != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.accent, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Text(text = statusToast ?: "", style = type.body, color = theme.accent)
                }
            }

            if (!hasCameraPermission) {
                GlassPermissionCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Camera Access for QR & Barcode",
                    explanation = "Dyne decodes QR, Data Matrix, Aztec, PDF417, EAN, UPC, Code 128, Code 39, Code 93, ITF, and Codabar barcodes 100% offline on your device. Camera frames never leave your phone.",
                    buttonText = "Allow Camera Access",
                    hazeState = hazeState,
                    onRequestPermission = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    secondaryActionText = "Scan from Gallery Image Instead",
                    onSecondaryAction = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .border(1.5.dp, theme.accent.copy(alpha = 0.65f), RoundedCornerShape(30.dp))
                ) {
                    CameraBarcodePreview(
                        torchEnabled = torchEnabled,
                        onBarcodeDetected = { raw, format ->
                            viewModel.onBarcodeDetected(
                                rawValue = raw,
                                format = format,
                                isPro = isPro,
                                onNewDetection = {
                                    FeedbackHelper.vibrateTick(context, 40L, 220)
                                    FeedbackHelper.playBeep(65)
                                }
                            )
                        }
                    )

                    GlassViewfinderOverlay(accentColor = theme.accent)

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassButton(
                            text = if (torchEnabled) "Torch On" else "Torch Off",
                            icon = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            accentFill = if (torchEnabled) theme.accent else null,
                            onClick = viewModel::toggleTorch,
                            modifier = Modifier.weight(1f),
                            testTag = "scan_torch_button"
                        )
                        GlassButton(
                            text = "Scan from Gallery",
                            icon = Icons.Default.PhotoLibrary,
                            accentFill = theme.bentoSky,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "scan_gallery_button"
                        )
                    }
                }
            }
        }
    }

    val currentRecord = activeResult
    if (currentRecord != null) {
        ScanResultGlassSheet(
            record = currentRecord,
            hazeState = hazeState,
            onToggleFavorite = { viewModel.toggleScanFavorite(currentRecord) },
            onDismiss = { viewModel.selectRecordForSheet(null) }
        )
    }
}

/**
 * Interactive Document Scanner:
 * 1) Attempts ML Kit Document Scanner (GmsDocumentScanning) with auto edge detection, perspective crop,
 *    filters, multi-page (limit 20), gallery import, and PDF + JPEG results.
 * 2) If GmsDocumentScanning is unavailable (or user switches to Manual Studio), falls back to
 *    CameraX capture + interactive manual 4-corner perspective crop screen with filter selector.
 * 3) After scanning, presents a glass "Save document" sheet with auto-generated name
 *    ("Scan 2026-10-24 14:32"), category chips (ID, Receipt, Bill, Notes, Other), and Save.
 */
@Composable
fun DocumentScannerScreen(
    viewModel: ScanViewModel,
    hazeState: HazeState?,
    onDocumentSaved: () -> Unit,
    onClose: () -> Unit,
    isDocsUnlocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val featureGate = LocalFeatureGate.current

    val maxPages = if (isDocsUnlocked) 50 else 20
    var filterIndex by remember { mutableIntStateOf(0) }
    val filterModes = remember {
        ProScanFilterMode.entries.map { it.label }
    }
    var idCardModeEnabled by remember { mutableStateOf(false) }

    fun requireDocPack(feature: DocumentPackFeature, onAllowed: () -> Unit) {
        if (isDocsUnlocked || featureGate == null) {
            onAllowed()
        } else {
            featureGate.requireDocumentPack(feature, onAllowed)
        }
    }

    // Captured pages waiting to be saved (max 20 Free / 50 Premium)
    val capturedPages = remember { mutableStateListOf<Bitmap>() }

    // Current bitmap being manually 4-corner cropped before adding to capturedPages
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cornerTL by remember { mutableStateOf(PointF(0.10f, 0.10f)) }
    var cornerTR by remember { mutableStateOf(PointF(0.90f, 0.10f)) }
    var cornerBR by remember { mutableStateOf(PointF(0.90f, 0.90f)) }
    var cornerBL by remember { mutableStateOf(PointF(0.10f, 0.90f)) }

    // Post-scan Save Document Glass Sheet state
    var showSaveDocumentSheet by remember { mutableStateOf(false) }
    var gmsStatusBanner by remember {
        mutableStateOf("Auto edge detection & perspective crop ready")
    }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // ML Kit GmsDocumentScanning Activity Result Launcher (RESULT_FORMAT_JPEG only, never scanner's PDF)
    val gmsScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            if (result != null) {
                capturedPages.clear()
                result.pages?.forEach { page ->
                    val bmp = ScanRepository.decodeUriWithExifRotation(context, page.imageUri)
                    if (bmp != null && capturedPages.size < maxPages) {
                        capturedPages.add(bmp)
                    }
                }
                if (capturedPages.isNotEmpty()) {
                    FeedbackHelper.vibrateTick(context, 40L, 210)
                    showSaveDocumentSheet = true
                }
            }
        }
    }

    fun launchGmsDocumentScanner() {
        val activity = context.findActivity()
        if (activity == null) {
            gmsStatusBanner = "Using built-in CameraX + 4-Corner Crop Studio"
            return
        }
        runCatching {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(maxPages)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
            val scanner = GmsDocumentScanning.getClient(options)
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    gmsScannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener {
                    gmsStatusBanner = "Play Services Scanner unavailable on this device — using CameraX + Manual 4-Corner Crop"
                }
        }.onFailure {
            gmsStatusBanner = "Using CameraX + Manual 4-Corner Crop Studio"
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = ScanRepository.decodeUriWithExifRotation(context, uri)
            if (decoded != null) {
                cornerTL = PointF(0.08f, 0.08f)
                cornerTR = PointF(0.92f, 0.08f)
                cornerBR = PointF(0.92f, 0.92f)
                cornerBL = PointF(0.08f, 0.92f)
                pendingCropBitmap = decoded
                FeedbackHelper.vibrateTick(context, 30L, 180)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .verticalScroll(rememberScrollState())
    ) {
        SharedTopBar(
            title = "Scan Document",
            subtitle = "Up to $maxPages pages",
            hazeState = hazeState,
            onBackClick = onClose
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status & Auto-Scanner Trigger Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.surface)
                    .border(1.dp, theme.bentoCoral.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto Edge Detection",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.bentoCoral
                    )
                    Text(
                        text = gmsStatusBanner,
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                GlassButton(
                    text = "Auto Scan",
                    icon = Icons.Default.AutoFixHigh,
                    accentFill = theme.bentoCoral,
                    onClick = { launchGmsDocumentScanner() },
                    testTag = "doc_gms_scanner_button"
                )
            }

            // ID Card Mode (Front and back on one page at real size 243x153 pt)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (idCardModeEnabled) theme.accent.copy(alpha = 0.16f) else theme.surface)
                    .border(
                        1.dp,
                        if (idCardModeEnabled) theme.accent else ProGold.copy(alpha = 0.45f),
                        RoundedCornerShape(20.dp)
                    )
                    .pressScaleClickable(
                        testTag = "doc_id_card_mode_toggle",
                        onClick = {
                            if (idCardModeEnabled) {
                                idCardModeEnabled = false
                            } else {
                                requireDocPack(DocumentPackFeature.SMART_ID_CARD_MODE) {
                                    idCardModeEnabled = true
                                }
                            }
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "ID Card Mode",
                        tint = if (idCardModeEnabled) theme.accent else ProGold
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ID Card Mode (Front & Back on 1 Page)",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                            if (!isDocsUnlocked) {
                                ProBadge()
                            }
                        }
                        Text(
                            text = if (idCardModeEnabled) {
                                when (capturedPages.size) {
                                    0 -> "Active: Capture ID Front (1/2) then ID Back (2/2) at real 243×153 pt size"
                                    1 -> "Front captured! Now capture ID Back (2/2) to combine on 1 page"
                                    else -> "Front + Back ready to combine on 1 page at real CR80 ID size"
                                }
                            } else {
                                "Places front & back of your ID card onto one page at real 85.6×54mm size"
                            },
                            style = type.caption,
                            color = if (idCardModeEnabled) theme.accent else theme.textSecondary
                        )
                    }
                }
                if (idCardModeEnabled && capturedPages.size >= 2) {
                    GlassButton(
                        text = "Combine 1P",
                        accentFill = theme.accent,
                        onClick = {
                            val composed = DocumentSmartTools.composeIdCardFrontAndBackPage(
                                frontBitmap = capturedPages[0],
                                backBitmap = capturedPages[1]
                            )
                            capturedPages.clear()
                            capturedPages.add(composed)
                            FeedbackHelper.vibrateTick(context, 35L, 200)
                        },
                        testTag = "doc_id_card_combine_button"
                    )
                }
            }

            if (!hasCameraPermission) {
                GlassPermissionCard(
                    icon = Icons.Default.DocumentScanner,
                    title = "Camera Access for Document Scanner",
                    explanation = "Dyne uses your camera locally for edge detection, perspective crop, and multi-page PDF creation (up to $maxPages pages). Full-resolution JPEGs stay in app-private storage.",
                    buttonText = "Allow Camera Access",
                    hazeState = hazeState,
                    onRequestPermission = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    secondaryActionText = "Import Page from Gallery Instead",
                    onSecondaryAction = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            } else if (pendingCropBitmap != null) {
                // Interactive Manual 4-Corner Perspective Crop Screen with Rotate button
                ManualCornerCropSection(
                    bitmap = pendingCropBitmap!!,
                    cornerTL = cornerTL,
                    cornerTR = cornerTR,
                    cornerBR = cornerBR,
                    cornerBL = cornerBL,
                    onUpdateCorners = { tl, tr, br, bl ->
                        cornerTL = tl
                        cornerTR = tr
                        cornerBR = br
                        cornerBL = bl
                    },
                    onRotate90 = {
                        pendingCropBitmap = ScanRepository.rotateBitmap90(pendingCropBitmap!!)
                        FeedbackHelper.hapticTick(context)
                    },
                    onCancelCrop = { pendingCropBitmap = null },
                    onConfirmCrop = {
                        val cropped = ScanRepository.applyFilterToBitmap(
                            source = pendingCropBitmap!!,
                            filterMode = "Original Color",
                            cropCornersNormalized = listOf(cornerTL, cornerTR, cornerBR, cornerBL)
                        )
                        if (capturedPages.size < maxPages) {
                            capturedPages.add(cropped)
                            if (idCardModeEnabled && capturedPages.size == 2) {
                                val combined = DocumentSmartTools.composeIdCardFrontAndBackPage(
                                    frontBitmap = capturedPages[0],
                                    backBitmap = capturedPages[1]
                                )
                                capturedPages.clear()
                                capturedPages.add(combined)
                            }
                        }
                        pendingCropBitmap = null
                        FeedbackHelper.vibrateTick(context, 35L, 200)
                    }
                )
            } else {
                // Live CameraX Document Viewfinder with Edge Guides
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.5.dp, theme.bentoCoral, RoundedCornerShape(28.dp))
                ) {
                    CameraBarcodePreview(
                        torchEnabled = false,
                        onPreviewViewReady = { previewViewRef = it },
                        onBarcodeDetected = { _, _ -> }
                    )
                    GlassViewfinderOverlay(accentColor = theme.bentoCoral)

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${capturedPages.size} / $maxPages PAGES",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                    }
                }
            }

            // Enhancement filter selector: Original Color (Free), Magic Color, B&W Adaptive, Grayscale, Whiteboard, Shadow Removal (Pro)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                ProScanFilterMode.entries.forEachIndexed { idx, mode ->
                    val selected = filterIndex == idx
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) theme.accent else theme.surface)
                            .border(
                                1.dp,
                                if (selected) theme.accent else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .pressScaleClickable(
                                testTag = "scanner_filter_chip_${mode.name.lowercase()}",
                                onClick = {
                                    if (mode.isPremium && !isDocsUnlocked) {
                                        requireDocPack(DocumentPackFeature.SMART_SCAN_FILTERS) {
                                            filterIndex = idx
                                        }
                                    } else {
                                        filterIndex = idx
                                    }
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = mode.label,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) BentoTextBlack else theme.textPrimary
                        )
                        if (mode.isPremium && !isDocsUnlocked) {
                            ProBadge()
                        }
                    }
                }
            }

            // Multi-page strip if user has captured 1+ pages (with Rotate button)
            if (capturedPages.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    capturedPages.forEachIndexed { idx, bmp ->
                        Box(
                            modifier = Modifier
                                .size(width = 82.dp, height = 108.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, theme.accent, RoundedCornerShape(14.dp))
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "P${idx + 1}",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "↻ 90°",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = theme.accent,
                                    modifier = Modifier.pressScaleClickable(
                                        testTag = "rotate_captured_page_$idx",
                                        onClick = {
                                            capturedPages[idx] = ScanRepository.rotateBitmap90(bmp)
                                            FeedbackHelper.hapticTick(context)
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons: Gallery Import, Capture + Corner Crop, Finish & Save
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassButton(
                    text = "Gallery",
                    icon = Icons.Default.PhotoLibrary,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "doc_import_gallery_button"
                )

                GlassButton(
                    text = if (idCardModeEnabled) {
                        if (capturedPages.isEmpty()) "Capture ID Front" else "Capture ID Back"
                    } else {
                        if (capturedPages.isEmpty()) "Capture & Crop" else "+ Add Page"
                    },
                    icon = Icons.Default.AddAPhoto,
                    accentFill = theme.bentoCoral,
                    onClick = {
                        FeedbackHelper.vibrateTick(context, 30L, 180)
                        val liveBitmap = previewViewRef?.bitmap
                            ?: ScanRepository.generateSampleDocumentBitmap(
                                title = ScanRepository.defaultDocumentName(),
                                category = if (idCardModeEnabled) "ID" else "Document",
                                filterMode = "Original Color",
                                pageNumber = capturedPages.size + 1
                            )
                        cornerTL = PointF(0.08f, 0.08f)
                        cornerTR = PointF(0.92f, 0.08f)
                        cornerBR = PointF(0.92f, 0.92f)
                        cornerBL = PointF(0.08f, 0.92f)
                        pendingCropBitmap = liveBitmap
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "doc_capture_crop_button"
                )
            }

            GlassButton(
                text = if (capturedPages.isEmpty()) {
                    "Quick Capture & Save Document"
                } else {
                    "Save Document (${capturedPages.size} ${if (capturedPages.size == 1) "Page" else "Pages"})"
                },
                icon = Icons.Default.CameraAlt,
                accentFill = theme.accent,
                onClick = {
                    FeedbackHelper.vibrateTick(context, 35L, 200)
                    FeedbackHelper.playBeep(50)
                    if (capturedPages.isEmpty()) {
                        val liveBitmap = previewViewRef?.bitmap
                            ?: ScanRepository.generateSampleDocumentBitmap(
                                title = ScanRepository.defaultDocumentName(),
                                category = if (idCardModeEnabled) "ID" else "Receipt",
                                filterMode = "Original Color",
                                pageNumber = 1
                            )
                        if (idCardModeEnabled) {
                            val backBmp = ScanRepository.generateSampleDocumentBitmap(
                                title = "${ScanRepository.defaultDocumentName()} (Back)",
                                category = "ID",
                                filterMode = "Original Color",
                                pageNumber = 2
                            )
                            capturedPages.add(
                                DocumentSmartTools.composeIdCardFrontAndBackPage(liveBitmap, backBmp)
                            )
                        } else {
                            capturedPages.add(liveBitmap)
                        }
                    } else if (idCardModeEnabled && capturedPages.size >= 2) {
                        val combined = DocumentSmartTools.composeIdCardFrontAndBackPage(
                            frontBitmap = capturedPages[0],
                            backBitmap = capturedPages[1]
                        )
                        capturedPages.clear()
                        capturedPages.add(combined)
                    }
                    showSaveDocumentSheet = true
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "doc_capture_save_button"
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showSaveDocumentSheet) {
        SaveDocumentGlassSheet(
            capturedPages = capturedPages.toList(),
            hazeState = hazeState,
            onSave = { docName, category, pageSize, fitMode, marginMode, qualityDpi ->
                showSaveDocumentSheet = false
                viewModel.createAndSaveDocument(
                    context = context,
                    name = docName,
                    category = if (idCardModeEnabled) "ID" else category,
                    filterMode = filterModes.getOrElse(filterIndex) { "Original Color" },
                    pageBitmaps = capturedPages.toList(),
                    pageSize = pageSize,
                    fitMode = fitMode,
                    marginMode = marginMode,
                    qualityDpi = qualityDpi,
                    pageLimit = maxPages,
                    onComplete = {
                        onDocumentSaved()
                    }
                )
            },
            onDismiss = { showSaveDocumentSheet = false }
        )
    }
}

/**
 * Interactive 4-Corner Manual Crop Screen with draggable handles and a Rotate 90° button.
 */
@Composable
private fun ManualCornerCropSection(
    bitmap: Bitmap,
    cornerTL: PointF,
    cornerTR: PointF,
    cornerBR: PointF,
    cornerBL: PointF,
    onUpdateCorners: (PointF, PointF, PointF, PointF) -> Unit,
    onRotate90: () -> Unit,
    onCancelCrop: () -> Unit,
    onConfirmCrop: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(theme.surface)
            .border(1.5.dp, theme.accent, RoundedCornerShape(26.dp))
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Crop,
                    contentDescription = "Corner Crop",
                    tint = theme.accent
                )
                Text(
                    text = "Drag 4 Corners to Crop Page",
                    style = type.section,
                    color = theme.textPrimary
                )
            }
            Text(
                text = "↻ Rotate 90°",
                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                color = theme.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.accent.copy(alpha = 0.14f))
                    .pressScaleClickable(
                        testTag = "doc_rotate_crop_button",
                        onClick = onRotate90
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured Page for Corner Crop",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val nx = (change.position.x / w).coerceIn(0.02f, 0.98f)
                            val ny = (change.position.y / h).coerceIn(0.02f, 0.98f)

                            val corners = listOf(cornerTL, cornerTR, cornerBR, cornerBL)
                            val nearestIdx = corners.indices.minByOrNull { idx ->
                                hypot(corners[idx].x - nx, corners[idx].y - ny)
                            } ?: 0

                            when (nearestIdx) {
                                0 -> onUpdateCorners(PointF(nx, ny), cornerTR, cornerBR, cornerBL)
                                1 -> onUpdateCorners(cornerTL, PointF(nx, ny), cornerBR, cornerBL)
                                2 -> onUpdateCorners(cornerTL, cornerTR, PointF(nx, ny), cornerBL)
                                else -> onUpdateCorners(cornerTL, cornerTR, cornerBR, PointF(nx, ny))
                            }
                        }
                    }
            ) {
                val pTL = Offset(cornerTL.x * size.width, cornerTL.y * size.height)
                val pTR = Offset(cornerTR.x * size.width, cornerTR.y * size.height)
                val pBR = Offset(cornerBR.x * size.width, cornerBR.y * size.height)
                val pBL = Offset(cornerBL.x * size.width, cornerBL.y * size.height)

                val quadPath = Path().apply {
                    moveTo(pTL.x, pTL.y)
                    lineTo(pTR.x, pTR.y)
                    lineTo(pBR.x, pBR.y)
                    lineTo(pBL.x, pBL.y)
                    close()
                }

                drawPath(
                    path = quadPath,
                    color = theme.accent.copy(alpha = 0.16f)
                )
                drawPath(
                    path = quadPath,
                    color = theme.accent,
                    style = Stroke(width = 3.dp.toPx())
                )

                listOf(pTL, pTR, pBR, pBL).forEach { pt ->
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.65f),
                        radius = 14.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = theme.accent,
                        radius = 10.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Retake",
                onClick = onCancelCrop,
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Apply Crop & Add Page",
                icon = Icons.Default.Check,
                accentFill = theme.accent,
                onClick = onConfirmCrop,
                modifier = Modifier.weight(1f),
                testTag = "doc_apply_crop_button"
            )
        }
    }
}

/**
 * Post-scan glass "Save document" sheet with auto-generated name ("Scan 2026-10-24 14:32"),
 * category chips (ID, Receipt, Bill, Notes, Other), Page Size selector (with ±3% auto-detect chip),
 * Fit/Fill + Margin selector, Quality DPI selector with live estimated file size, and live Page-Size preview.
 */
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaveDocumentGlassSheet(
    capturedPages: List<Bitmap>,
    hazeState: HazeState?,
    onSave: (
        name: String,
        category: String,
        pageSize: PdfPageSize,
        fitMode: PdfFitMode,
        marginMode: PdfMarginMode,
        qualityDpi: PdfQualityDpi
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val pageCount = capturedPages.size.coerceAtLeast(1)
    val firstBmp = capturedPages.firstOrNull()
    val firstW = firstBmp?.width ?: 800
    val firstH = firstBmp?.height ?: 1120

    val detectedSize = remember(firstW, firstH) {
        PdfPageSize.detectClosestStandardSize(firstW, firstH)
    }

    var docName by remember { mutableStateOf(ScanRepository.defaultDocumentName()) }
    var selectedCategory by remember { mutableStateOf("Receipt") }
    val categories = remember { listOf("ID", "Receipt", "Bill", "Notes", "Other") }

    var selectedPageSize by remember { mutableStateOf(PdfPageSize.defaultForLocale()) }
    var selectedFitMode by remember { mutableStateOf(PdfFitMode.FIT) }
    var selectedMarginMode by remember { mutableStateOf(PdfMarginMode.SMALL_10MM) }
    var selectedQualityDpi by remember { mutableStateOf(PdfQualityDpi.HIGH_200) }

    val estimatedBytes = remember(capturedPages, selectedPageSize, selectedQualityDpi) {
        ScanRepository.estimatePdfSizeBytes(capturedPages, selectedPageSize, selectedQualityDpi)
    }
    val estimatedSizeLabel = remember(estimatedBytes) {
        ScanRepository.formatFileSize(estimatedBytes)
    }

    GlassSheet(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Save Document",
                        style = type.title,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "$pageCount ${if (pageCount == 1) "page" else "pages"} • Est. $estimatedSizeLabel • Full-res JPEG + custom PDF",
                        style = type.caption,
                        color = theme.accent
                    )
                }
            }

            if (detectedSize != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.accent.copy(alpha = 0.14f))
                        .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .pressScaleClickable(
                            testTag = "auto_detect_page_size_chip",
                            onClick = { selectedPageSize = detectedSize }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Looks like ${detectedSize.label}",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                    Text(
                        text = if (selectedPageSize == detectedSize) "Selected ✓" else "Tap to apply",
                        style = type.caption,
                        color = theme.textPrimary
                    )
                }
            }

            OutlinedTextField(
                value = docName,
                onValueChange = { docName = it },
                label = { Text("Document Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "CATEGORY",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
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
                                testTag = "doc_category_chip_${category.lowercase()}",
                                onClick = { selectedCategory = category }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) BentoTextBlack else theme.textPrimary
                        )
                    }
                }
            }

            // Live Page Size & Margin Preview + Controls
            DocumentPageSizePreviewCard(
                sourceWidth = firstW,
                sourceHeight = firstH,
                pageSize = selectedPageSize,
                fitMode = selectedFitMode,
                marginMode = selectedMarginMode,
                qualityDpi = selectedQualityDpi,
                estimatedSizeLabel = estimatedSizeLabel
            )

            Text(
                text = "PAGE SIZE",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )

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
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) theme.accent else theme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) theme.accent else Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .pressScaleClickable(
                                testTag = "page_size_chip_${sizeOption.id}",
                                onClick = { selectedPageSize = sizeOption }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = sizeOption.label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) BentoTextBlack else theme.textPrimary
                            )
                            Text(
                                text = sizeOption.subtitle,
                                style = type.caption,
                                color = if (isSelected) BentoTextBlack.copy(alpha = 0.75f) else theme.textSecondary
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                PdfFitMode.entries.forEach { mode ->
                    val isSelected = selectedFitMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accent else theme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) theme.accent else Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .pressScaleClickable(
                                testTag = "fit_mode_chip_${mode.id}",
                                onClick = { selectedFitMode = mode }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (mode == PdfFitMode.FIT) "Fit (Margin)" else "Fill Page",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) BentoTextBlack else theme.textPrimary
                        )
                    }
                }
            }

            if (selectedFitMode == PdfFitMode.FIT) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PdfMarginMode.entries.forEach { margin ->
                        val isSelected = selectedMarginMode == margin
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) theme.bentoSky else theme.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) theme.bentoSky else Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .pressScaleClickable(
                                    testTag = "margin_mode_chip_${margin.id}",
                                    onClick = { selectedMarginMode = margin }
                                )
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = margin.label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "QUALITY & ESTIMATED SIZE",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )
                Text(
                    text = "~$estimatedSizeLabel",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                PdfQualityDpi.entries.forEach { dpiOption ->
                    val isSelected = selectedQualityDpi == dpiOption
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accent else theme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) theme.accent else Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .pressScaleClickable(
                                testTag = "quality_dpi_chip_${dpiOption.id}",
                                onClick = { selectedQualityDpi = dpiOption }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = dpiOption.label,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) BentoTextBlack else theme.textPrimary
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Save",
                    icon = Icons.Default.Check,
                    accentFill = theme.accent,
                    onClick = {
                        onSave(
                            docName.ifBlank { ScanRepository.defaultDocumentName() },
                            selectedCategory,
                            selectedPageSize,
                            selectedFitMode,
                            selectedMarginMode,
                            selectedQualityDpi
                        )
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "save_document_confirm_button"
                )
            }
        }
    }
}

/**
 * Live Page-Size preview card used in both SaveDocumentGlassSheet and DocumentViewerScreen.
 * Shows the exact page aspect ratio (portrait or landscape), white page background, margin guide,
 * and centered aspect-preserving scan placement (never stretched).
 */
@Composable
fun DocumentPageSizePreviewCard(
    sourceWidth: Int,
    sourceHeight: Int,
    pageSize: PdfPageSize,
    fitMode: PdfFitMode,
    marginMode: PdfMarginMode,
    qualityDpi: PdfQualityDpi,
    estimatedSizeLabel: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val (pageW, pageH) = remember(pageSize, sourceWidth, sourceHeight) {
        pageSize.resolvePageDimensionsPt(sourceWidth, sourceHeight)
    }
    val orientationLabel = if (pageW > pageH) "Landscape" else "Portrait"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 68.dp, height = 84.dp)
        ) {
            val availCanvasW = size.width
            val availCanvasH = size.height
            val pageScale = min(availCanvasW / pageW.toFloat(), availCanvasH / pageH.toFloat())
            val drawnPageW = pageW * pageScale
            val drawnPageH = pageH * pageScale
            val pageLeft = (availCanvasW - drawnPageW) / 2f
            val pageTop = (availCanvasH - drawnPageH) / 2f

            // White paper sheet
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(pageLeft, pageTop),
                size = androidx.compose.ui.geometry.Size(drawnPageW, drawnPageH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            val marginPt = if (fitMode == PdfFitMode.FILL) 0f else marginMode.marginPt.toFloat()
            val innerW = (pageW - marginPt * 2f).coerceAtLeast(36f)
            val innerH = (pageH - marginPt * 2f).coerceAtLeast(36f)

            val srcW = sourceWidth.toFloat().coerceAtLeast(1f)
            val srcH = sourceHeight.toFloat().coerceAtLeast(1f)
            val imgScale = if (fitMode == PdfFitMode.FILL) {
                max(innerW / srcW, innerH / srcH)
            } else {
                min(innerW / srcW, innerH / srcH)
            }

            val imgW = (srcW * imgScale).coerceAtMost(pageW.toFloat()) * pageScale
            val imgH = (srcH * imgScale).coerceAtMost(pageH.toFloat()) * pageScale
            val imgLeft = pageLeft + (drawnPageW - imgW) / 2f
            val imgTop = pageTop + (drawnPageH - imgH) / 2f

            // Centered scan content block (never stretched)
            drawRoundRect(
                color = theme.accent.copy(alpha = 0.55f),
                topLeft = Offset(imgLeft, imgTop),
                size = androidx.compose.ui.geometry.Size(imgW, imgH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRoundRect(
                color = theme.bentoSky,
                topLeft = Offset(imgLeft, imgTop),
                size = androidx.compose.ui.geometry.Size(imgW, imgH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${pageSize.label} ($orientationLabel)",
                style = type.body.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary
            )
            Text(
                text = "${pageW} × ${pageH} pt • ${if (fitMode == PdfFitMode.FILL) "Fill page" else "Fit (${marginMode.label} margin)"}",
                style = type.caption,
                color = theme.textSecondary
            )
            Text(
                text = "${qualityDpi.label} • Est. $estimatedSizeLabel",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.accent
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraBarcodePreview(
    torchEnabled: Boolean,
    onPreviewViewReady: (PreviewView) -> Unit = {},
    onBarcodeDetected: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(previewView) {
        onPreviewViewReady(previewView)
    }

    val scanner = remember {
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
        BarcodeScanning.getClient(options)
    }

    LaunchedEffect(torchEnabled, cameraRef) {
        runCatching {
            cameraRef?.cameraControl?.enableTorch(torchEnabled)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val hit = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                if (hit?.rawValue != null) {
                                    onBarcodeDetected(hit.rawValue!!, hit.format)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraRef = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun GlassViewfinderOverlay(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val boxSize = size.minDimension * 0.70f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f - 16.dp.toPx()
        val right = left + boxSize
        val bottom = top + boxSize
        val cornerLen = 34.dp.toPx()
        val stroke = 4.dp.toPx()

        drawRoundRect(
            color = Color.White.copy(alpha = 0.06f),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )

        drawLine(accentColor, Offset(left, top + cornerLen), Offset(left, top), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(left, top), Offset(left + cornerLen, top), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(right - cornerLen, top), Offset(right, top), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(right, top), Offset(right, top + cornerLen), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(left, bottom - cornerLen), Offset(left, bottom), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(left, bottom), Offset(left + cornerLen, bottom), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(right - cornerLen, bottom), Offset(right, bottom), stroke, StrokeCap.Round)
        drawLine(accentColor, Offset(right, bottom), Offset(right, bottom - cornerLen), stroke, StrokeCap.Round)

        val lineY = top + boxSize * scanLineProgress
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, accentColor, Color.White, accentColor, Color.Transparent),
                startX = left,
                endX = right
            ),
            start = Offset(left + 8.dp.toPx(), lineY),
            end = Offset(right - 8.dp.toPx(), lineY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultGlassSheet(
    record: ScanRecord,
    hazeState: HazeState?,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val contentType = record.parsedContentType

    GlassSheet(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${record.formatName} • ${contentType.displayLabel}",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                    Text(
                        text = record.displayTitle,
                        style = type.title,
                        color = theme.textPrimary
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (record.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (record.isFavorite) theme.accent else theme.textSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = record.rawContent,
                    style = type.body,
                    color = theme.textPrimary
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (contentType == ScanContentType.URL) {
                    GlassButton(
                        text = "Open Link",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        accentFill = theme.accent,
                        onClick = {
                            runCatching {
                                val url = if (record.rawContent.startsWith("http", ignoreCase = true)) {
                                    record.rawContent.trim()
                                } else {
                                    "https://${record.rawContent.trim()}"
                                }
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    )
                }
                if (contentType == ScanContentType.PHONE) {
                    GlassButton(
                        text = "Call",
                        icon = Icons.Default.Call,
                        accentFill = theme.accent,
                        onClick = {
                            runCatching {
                                val num = record.rawContent.removePrefix("tel:").removePrefix("TEL:").trim()
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")))
                            }
                        }
                    )
                }
                if (contentType == ScanContentType.EMAIL || contentType == ScanContentType.SMS) {
                    GlassButton(
                        text = if (contentType == ScanContentType.EMAIL) "Email" else "SMS",
                        icon = Icons.Default.Email,
                        accentFill = theme.accent,
                        onClick = {
                            runCatching {
                                val uri = if (contentType == ScanContentType.EMAIL) {
                                    Uri.parse("mailto:${record.displayTitle}")
                                } else {
                                    Uri.parse("smsto:${record.displayTitle}")
                                }
                                context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
                            }
                        }
                    )
                }
                if (contentType == ScanContentType.LOCATION) {
                    GlassButton(
                        text = "Open in Maps",
                        icon = Icons.Default.Map,
                        accentFill = theme.accent,
                        onClick = {
                            runCatching {
                                val geo = if (record.rawContent.startsWith("geo:", ignoreCase = true)) {
                                    Uri.parse(record.rawContent)
                                } else {
                                    Uri.parse("geo:${record.rawContent.trim()}")
                                }
                                context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                            }
                        }
                    )
                }

                GlassButton(
                    text = "Copy",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cb?.setPrimaryClip(ClipData.newPlainText("Scanned Code", record.rawContent))
                        FeedbackHelper.vibrateTick(context)
                    }
                )

                GlassButton(
                    text = "Share",
                    icon = Icons.Default.Share,
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            this.type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, record.rawContent)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Scan Result"))
                    }
                )
            }

            GlassButton(
                text = "Done",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }.getOrNull()
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
