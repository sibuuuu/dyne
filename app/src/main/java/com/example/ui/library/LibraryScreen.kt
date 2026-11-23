package com.example.ui.library

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.InlineBannerAdSlot
import com.example.ads.LocalAdManager
import com.example.ads.RewardUnlockPromptTarget
import com.example.billing.DocumentPackFeature
import com.example.billing.FeatureGate
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.billing.QuotaAllowanceChip
import com.example.billing.QuotaType
import com.example.data.BusinessCardParsedData
import com.example.data.DocumentRecord
import com.example.data.DocumentSmartTools
import com.example.data.PdfFitMode
import com.example.data.PdfMarginMode
import com.example.data.PdfPageSize
import com.example.data.PdfQualityDpi
import com.example.data.ProScanFilterMode
import com.example.data.ReceiptParsedData
import com.example.data.ScanRecord
import com.example.data.ScanRepository
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassSheet
import com.example.ui.components.GlassSurface
import com.example.ui.components.HazeState
import com.example.ui.components.LocalBottomBarInset
import com.example.ui.components.ProBadge
import com.example.ui.components.SharedTopBar
import com.example.ui.components.horizontalChipRowEdgeFade
import com.example.ui.components.pressScaleClickable
import com.example.ui.components.proBorder
import com.example.ui.notes.NotesWorkspaceSection
import com.example.ui.scan.DocumentCategoryFilter
import com.example.ui.scan.DocumentPageSizePreviewCard
import com.example.ui.scan.DocumentSortOrder
import com.example.ui.scan.ScanFilterCategory
import com.example.ui.scan.ScanResultGlassSheet
import com.example.ui.scan.ScanViewModel
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    selectedSegment: Int,
    onSegmentChange: (Int) -> Unit,
    scanViewModel: ScanViewModel,
    isPro: Boolean,
    bonusDocumentSaves: Int = 0,
    scrollToTopTrigger: Int = 0,
    hazeState: HazeState?,
    nestedScrollConnection: NestedScrollConnection,
    onLaunchDocumentScanner: () -> Unit,
    onLaunchQrScanner: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenPdfTools: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adManager = LocalAdManager.current
    val featureGate = LocalFeatureGate.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val freeDocLimit = 5 + bonusDocumentSaves
    val isDocsUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) == true)
    val isCsvUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.CSV_EXPORT) == true)
    val mainScrollState = rememberScrollState()

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            mainScrollState.animateScrollTo(0)
        }
    }

    val searchQuery by scanViewModel.searchQuery.collectAsStateWithLifecycle()
    val filterCategory by scanViewModel.selectedFilter.collectAsStateWithLifecycle()
    val docCategoryFilter by scanViewModel.selectedDocCategory.collectAsStateWithLifecycle()
    val docSortOrder by scanViewModel.selectedDocSort.collectAsStateWithLifecycle()
    val scans by scanViewModel.filteredScans.collectAsStateWithLifecycle()
    val documents by scanViewModel.filteredDocuments.collectAsStateWithLifecycle()
    val allDocuments by scanViewModel.allDocuments.collectAsStateWithLifecycle()
    val activeScanResult by scanViewModel.activeResultRecord.collectAsStateWithLifecycle()
    val activeViewerDoc by scanViewModel.activeViewerDocument.collectAsStateWithLifecycle()
    val recentlyDeletedDoc by scanViewModel.recentlyDeletedDocument.collectAsStateWithLifecycle()
    val ocrSheetDoc by scanViewModel.ocrSheetDoc.collectAsStateWithLifecycle()
    val ocrTextDraft by scanViewModel.ocrTextDraft.collectAsStateWithLifecycle()
    val isExtractingOcr by scanViewModel.isExtractingOcr.collectAsStateWithLifecycle()
    val statusToast by scanViewModel.statusToast.collectAsStateWithLifecycle()

    val selectedFolder by scanViewModel.selectedFolder.collectAsStateWithLifecycle()
    val selectedTag by scanViewModel.selectedTag.collectAsStateWithLifecycle()
    val isBulkSelectMode by scanViewModel.isBulkSelectMode.collectAsStateWithLifecycle()
    val selectedDocIds by scanViewModel.selectedDocIds.collectAsStateWithLifecycle()
    val ocrIsOnePagePreview by scanViewModel.ocrIsOnePagePreview.collectAsStateWithLifecycle()

    var docPendingRename by remember { mutableStateOf<DocumentRecord?>(null) }
    var showReceiptExpenseSheet by remember { mutableStateOf(false) }
    var showBusinessCardSheet by remember { mutableStateOf(false) }
    var showBulkFolderMoveSheet by remember { mutableStateOf(false) }
    val segments = remember { listOf("Documents", "Scans", "Notes") }

    fun requireDocPack(feature: DocumentPackFeature, onAllowed: () -> Unit) {
        if (isDocsUnlocked || featureGate == null) {
            onAllowed()
        } else {
            featureGate.requireDocumentPack(feature, onAllowed)
        }
    }

    if (activeViewerDoc != null) {
        DocumentViewerScreen(
            doc = activeViewerDoc!!,
            allDocuments = allDocuments,
            scanViewModel = scanViewModel,
            isPro = isDocsUnlocked,
            featureGate = featureGate,
            hazeState = hazeState,
            onOpenPaywall = {
                if (featureGate != null) {
                    featureGate.require(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE)
                } else {
                    onOpenPaywall()
                }
            },
            onClose = { scanViewModel.closeDocumentViewer() }
        )

        if (ocrSheetDoc != null) {
            OcrExtractGlassSheet(
                documentName = ocrSheetDoc!!.name,
                ocrText = ocrTextDraft,
                isExtracting = isExtractingOcr,
                isOnePagePreview = ocrIsOnePagePreview,
                hazeState = hazeState,
                onUnlockFullOcr = {
                    requireDocPack(DocumentPackFeature.SMART_OCR_ACTIONS) {
                        scanViewModel.launchExtractOcrSheet(context, ocrSheetDoc!!, onePagePreviewOnly = false)
                    }
                },
                onRequireOcrAction = { onAllowed ->
                    requireDocPack(DocumentPackFeature.SMART_OCR_ACTIONS, onAllowed)
                },
                onTextChange = scanViewModel::updateOcrDraft,
                onSave = scanViewModel::saveOcrDraft,
                onDismiss = scanViewModel::closeOcrSheet
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .verticalScroll(mainScrollState)
    ) {
        // Header with Title + PDF Tools button
        SharedTopBar(
            title = "Library",
            subtitle = "ON-DEVICE ROOM VAULT",
            hazeState = hazeState,
            rightExtraContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .pressScaleClickable(
                            testTag = "library_pdf_tools_button",
                            onClick = onOpenPdfTools
                        )
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Tools",
                        tint = theme.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PDF Tools",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            rightActionIcon = Icons.Default.WorkspacePremium,
            rightActionDescription = "Dyne Pro",
            onRightActionClick = onOpenPaywall
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Full-width segmented control (Documents | Scans | Notes), 44dp
            DyneSegmentedControl(
                segments = segments,
                selectedIndex = selectedSegment,
                onSegmentSelected = onSegmentChange,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth()
            )

            if (statusToast != null) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = statusToast ?: "",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = theme.textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .pressScaleClickable(onClick = scanViewModel::clearToast)
                    )
                }
            }

            if (selectedSegment != 2) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = scanViewModel::setSearchQuery,
                    placeholder = {
                        Text(
                            text = if (selectedSegment == 0) {
                                "Search documents"
                            } else {
                                "Search scans"
                            },
                            style = type.body,
                            color = theme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = theme.accent)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = theme.textSecondary,
                                modifier = Modifier.pressScaleClickable(
                                    onClick = { scanViewModel.setSearchQuery("") }
                                )
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = theme.surface,
                        unfocusedContainerColor = theme.surface,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input")
                )
            }

            when (selectedSegment) {
                0 -> DocumentsVaultSection(
                    documents = documents,
                    allDocuments = allDocuments,
                    selectedCategory = docCategoryFilter,
                    selectedSort = docSortOrder,
                    selectedFolder = selectedFolder,
                    selectedTag = selectedTag,
                    isBulkSelectMode = isBulkSelectMode,
                    selectedDocIds = selectedDocIds,
                    recentlyDeletedDoc = recentlyDeletedDoc,
                    onSelectCategory = scanViewModel::setDocCategoryFilter,
                    onSelectSort = scanViewModel::setDocSortOrder,
                    onSelectFolder = { folder ->
                        if (folder == "All") {
                            scanViewModel.setSelectedFolder("All")
                        } else {
                            requireDocPack(DocumentPackFeature.SMART_LIBRARY_ORG) {
                                scanViewModel.setSelectedFolder(folder)
                            }
                        }
                    },
                    onSelectTag = { tag ->
                        if (tag == "All") {
                            scanViewModel.setSelectedTag("All")
                        } else {
                            requireDocPack(DocumentPackFeature.SMART_LIBRARY_ORG) {
                                scanViewModel.setSelectedTag(tag)
                            }
                        }
                    },
                    onToggleBulkSelect = {
                        if (isBulkSelectMode) {
                            scanViewModel.toggleBulkSelectMode(false)
                        } else {
                            requireDocPack(DocumentPackFeature.SMART_LIBRARY_ORG) {
                                scanViewModel.toggleBulkSelectMode(true)
                            }
                        }
                    },
                    onToggleSelectDocId = scanViewModel::toggleDocumentSelection,
                    onSelectAllDocs = scanViewModel::selectAllFilteredDocuments,
                    onBulkDelete = scanViewModel::bulkDeleteSelectedDocuments,
                    onBulkMoveFolder = { showBulkFolderMoveSheet = true },
                    onBulkMerge = {
                        val maxAllowed = if (isDocsUnlocked || featureGate?.isDocumentPackUnlocked(DocumentPackFeature.PDF_UNLIMITED_MERGE) == true) 50 else 3
                        scanViewModel.bulkMergeSelectedDocuments(context, maxAllowed)
                    },
                    onBulkExportZip = { saveToDownloads ->
                        requireDocPack(DocumentPackFeature.SMART_LIBRARY_ORG) {
                            scanViewModel.bulkExportSelectedAsZip(context, saveToDownloads) { chooser ->
                                context.startActivity(chooser)
                            }
                        }
                    },
                    onOpenReceiptExpenseCenter = {
                        requireDocPack(DocumentPackFeature.SMART_RECEIPT_EXPENSE) {
                            showReceiptExpenseSheet = true
                        }
                    },
                    onOpenBusinessCardCenter = {
                        requireDocPack(DocumentPackFeature.SMART_BUSINESS_CARD) {
                            showBusinessCardSheet = true
                        }
                    },
                    onScanNewDocument = {
                        if (!isDocsUnlocked && allDocuments.size >= freeDocLimit) {
                            if (featureGate != null) {
                                featureGate.require(
                                    feature = PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE,
                                    onUnlocked = onLaunchDocumentScanner
                                )
                            } else if (adManager != null) {
                                adManager.promptRewardedUnlock(
                                    RewardUnlockPromptTarget.ExtraDocumentSave(
                                        onUnlocked = onLaunchDocumentScanner
                                    )
                                )
                            } else {
                                onOpenPaywall()
                            }
                        } else {
                            onLaunchDocumentScanner()
                        }
                    },
                    onOpenDocument = { doc ->
                        if (isBulkSelectMode) {
                            scanViewModel.toggleDocumentSelection(doc.id)
                        } else if (doc.locked) {
                            authenticateWithBiometrics(
                                context = context,
                                title = "Unlock Document",
                                subtitle = "Authenticate to view \"${doc.name}\"",
                                onAuthenticated = {
                                    scanViewModel.openDocumentViewer(doc)
                                },
                                onInfoMessage = { msg ->
                                    scanViewModel.showToast(msg)
                                }
                            )
                        } else {
                            scanViewModel.openDocumentViewer(doc)
                        }
                    },
                    onToggleFavorite = scanViewModel::toggleDocumentFavorite,
                    onRequestRename = { doc -> docPendingRename = doc },
                    onShareDocument = { doc ->
                        scanViewModel.shareDocumentPdf(context, doc) { chooser ->
                            context.startActivity(chooser)
                        }
                    },
                    onSaveToPhone = { doc ->
                        scanViewModel.saveDocumentToPhone(context, doc)
                    },
                    onToggleLock = { doc ->
                        if (!isDocsUnlocked && !doc.locked) {
                            if (featureGate != null) {
                                featureGate.require(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE)
                            } else {
                                onOpenPaywall()
                            }
                        } else {
                            authenticateWithBiometrics(
                                context = context,
                                title = if (doc.locked) "Unlock Document" else "Lock Document",
                                subtitle = "Use fingerprint, face, or device PIN",
                                onAuthenticated = {
                                    scanViewModel.setDocumentLocked(doc, !doc.locked)
                                },
                                onInfoMessage = { msg ->
                                    scanViewModel.showToast(msg)
                                }
                            )
                        }
                    },
                    onDeleteDocument = scanViewModel::deleteDocumentWithUndo,
                    onUndoDelete = scanViewModel::undoDeleteDocument,
                    onDismissUndo = scanViewModel::dismissUndoBanner
                )

                1 -> ScansVaultSection(
                    scans = scans,
                    isPro = isCsvUnlocked,
                    filterCategory = filterCategory,
                    onSelectFilter = scanViewModel::setFilter,
                    onScanNewCode = onLaunchQrScanner,
                    onExportCsv = {
                        if (!isCsvUnlocked) {
                            if (featureGate != null) {
                                featureGate.require(PremiumFeature.CSV_EXPORT) {
                                    scanViewModel.exportCsv(context) { shareIntent ->
                                        context.startActivity(shareIntent)
                                    }
                                }
                            } else if (adManager != null) {
                                adManager.promptRewardedUnlock(
                                    RewardUnlockPromptTarget.OneCsvExport(
                                        onUnlocked = {
                                            scanViewModel.exportCsv(context) { shareIntent ->
                                                context.startActivity(shareIntent)
                                            }
                                        }
                                    )
                                )
                            } else {
                                onOpenPaywall()
                            }
                        } else {
                            scanViewModel.exportCsv(context) { shareIntent ->
                                context.startActivity(shareIntent)
                            }
                        }
                    },
                    onOpenPaywall = onOpenPaywall,
                    onInspectScan = { record -> scanViewModel.selectRecordForSheet(record) },
                    onToggleFavorite = scanViewModel::toggleScanFavorite,
                    onDelete = { scan -> scanViewModel.deleteScan(scan.id) }
                )

                else -> NotesWorkspaceSection(
                    scanViewModel = scanViewModel,
                    hazeState = hazeState,
                    isPro = isPro,
                    onOpenPaywall = onOpenPaywall
                )
            }

            // Generous bottom inset so nothing hides behind the floating tab bar
            Spacer(modifier = Modifier.height(LocalBottomBarInset.current + 36.dp))
        }
    }

    if (docPendingRename != null) {
        val target = docPendingRename!!
        RenameDocumentGlassSheet(
            currentName = target.name,
            hazeState = hazeState,
            onRename = { newName ->
                scanViewModel.renameDocument(target.id, newName)
                docPendingRename = null
            },
            onDismiss = { docPendingRename = null }
        )
    }

    if (activeScanResult != null) {
        ScanResultGlassSheet(
            record = activeScanResult!!,
            hazeState = hazeState,
            onToggleFavorite = { scanViewModel.toggleScanFavorite(activeScanResult!!) },
            onDismiss = { scanViewModel.selectRecordForSheet(null) }
        )
    }

    if (ocrSheetDoc != null) {
        OcrExtractGlassSheet(
            documentName = ocrSheetDoc!!.name,
            ocrText = ocrTextDraft,
            isExtracting = isExtractingOcr,
            isOnePagePreview = ocrIsOnePagePreview,
            hazeState = hazeState,
            onUnlockFullOcr = {
                requireDocPack(DocumentPackFeature.SMART_OCR_ACTIONS) {
                    scanViewModel.launchExtractOcrSheet(context, ocrSheetDoc!!, onePagePreviewOnly = false)
                }
            },
            onRequireOcrAction = { onAllowed ->
                requireDocPack(DocumentPackFeature.SMART_OCR_ACTIONS, onAllowed)
            },
            onTextChange = scanViewModel::updateOcrDraft,
            onSave = scanViewModel::saveOcrDraft,
            onDismiss = scanViewModel::closeOcrSheet
        )
    }

    if (showReceiptExpenseSheet) {
        ReceiptExpenseCenterGlassSheet(
            documents = allDocuments,
            scanViewModel = scanViewModel,
            hazeState = hazeState,
            onDismiss = { showReceiptExpenseSheet = false }
        )
    }

    if (showBusinessCardSheet) {
        BusinessCardCenterGlassSheet(
            documents = allDocuments,
            scanViewModel = scanViewModel,
            hazeState = hazeState,
            onDismiss = { showBusinessCardSheet = false }
        )
    }

    if (showBulkFolderMoveSheet) {
        var folderInput by remember { mutableStateOf("") }
        GlassSheet(
            onDismissRequest = { showBulkFolderMoveSheet = false },
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
                    Text("Move Selected to Folder", style = type.title, color = theme.textPrimary)
                    ProBadge()
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf("Work", "Taxes", "Medical", "Receipts", "Personal", "IDs").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.surface)
                                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .pressScaleClickable { folderInput = preset }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(preset, style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                        }
                    }
                }
                OutlinedTextField(
                    value = folderInput,
                    onValueChange = { folderInput = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    GlassButton(
                        text = "Cancel",
                        onClick = { showBulkFolderMoveSheet = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Move Documents",
                        icon = Icons.Default.Folder,
                        accentFill = theme.accent,
                        onClick = {
                            scanViewModel.bulkMoveSelectedToFolder(folderInput)
                            showBulkFolderMoveSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentsVaultSection(
    documents: List<DocumentRecord>,
    allDocuments: List<DocumentRecord>,
    selectedCategory: DocumentCategoryFilter,
    selectedSort: DocumentSortOrder,
    selectedFolder: String,
    selectedTag: String,
    isBulkSelectMode: Boolean,
    selectedDocIds: Set<Long>,
    recentlyDeletedDoc: DocumentRecord?,
    onSelectCategory: (DocumentCategoryFilter) -> Unit,
    onSelectSort: (DocumentSortOrder) -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onToggleBulkSelect: () -> Unit,
    onToggleSelectDocId: (Long) -> Unit,
    onSelectAllDocs: () -> Unit,
    onBulkDelete: () -> Unit,
    onBulkMoveFolder: () -> Unit,
    onBulkMerge: () -> Unit,
    onBulkExportZip: (Boolean) -> Unit,
    onOpenReceiptExpenseCenter: () -> Unit,
    onOpenBusinessCardCenter: () -> Unit,
    onScanNewDocument: () -> Unit,
    onOpenDocument: (DocumentRecord) -> Unit,
    onToggleFavorite: (DocumentRecord) -> Unit,
    onRequestRename: (DocumentRecord) -> Unit,
    onShareDocument: (DocumentRecord) -> Unit,
    onSaveToPhone: (DocumentRecord) -> Unit,
    onToggleLock: (DocumentRecord) -> Unit,
    onDeleteDocument: (DocumentRecord) -> Unit,
    onUndoDelete: () -> Unit,
    onDismissUndo: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var showSortDropdown by remember { mutableStateOf(false) }

    val availableFolders = remember(allDocuments) {
        val custom = allDocuments.map { it.folderName.trim() }.filter { it.isNotBlank() }.distinct()
        (listOf("All") + (custom + listOf("Work", "Taxes", "Receipts")).distinct())
    }
    val availableTags = remember(allDocuments) {
        val tags = allDocuments.flatMap { it.tagsList }.distinct()
        (listOf("All") + (tags + listOf("urgent", "tax", "expiring")).distinct())
    }

    if (recentlyDeletedDoc != null) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.elevated)
                .border(1.dp, theme.bentoCoral, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Deleted \"${recentlyDeletedDoc.name}\"",
                style = type.body,
                color = theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    text = "Undo",
                    icon = Icons.Default.Undo,
                    accentFill = theme.accent,
                    onClick = onUndoDelete,
                    testTag = "library_undo_delete_button"
                )
                IconButton(onClick = onDismissUndo, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Undo",
                        tint = theme.textSecondary
                    )
                }
            }
        }
    }

    // LazyRow of chips (All, Favorites, ID, Receipt, Bill, Notes, Other) with an edge fade
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalChipRowEdgeFade()
    ) {
        items(DocumentCategoryFilter.entries) { cat ->
            val selected = selectedCategory == cat
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) theme.accent else theme.surface)
                    .border(
                        1.dp,
                        if (selected) theme.accent else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp)
                    )
                    .pressScaleClickable(
                        testTag = "doc_filter_chip_${cat.name.lowercase()}",
                        onClick = { onSelectCategory(cat) }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = cat.label,
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) BentoTextBlack else theme.textSecondary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }

    // Smart Tools Row: Receipt & Expense, Business Card, Bulk Select, Batch ZIP
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        SmartLibraryChip(
            label = "Receipts & Expenses",
            icon = Icons.Default.ReceiptLong,
            isProUnlocked = true,
            isActive = false,
            testTag = "library_smart_receipts_button",
            onClick = onOpenReceiptExpenseCenter
        )
        SmartLibraryChip(
            label = "Business Card → Contacts",
            icon = Icons.Default.ContactPage,
            isProUnlocked = true,
            isActive = false,
            testTag = "library_smart_business_card_button",
            onClick = onOpenBusinessCardCenter
        )
        SmartLibraryChip(
            label = if (isBulkSelectMode) "Done Select (${selectedDocIds.size})" else "Bulk Select",
            icon = Icons.Default.Checklist,
            isProUnlocked = true,
            isActive = isBulkSelectMode,
            testTag = "library_smart_bulk_select_button",
            onClick = onToggleBulkSelect
        )
        SmartLibraryChip(
            label = "Export ZIP",
            icon = Icons.Default.FolderZip,
            isProUnlocked = true,
            isActive = false,
            testTag = "library_smart_export_zip_button",
            onClick = { onBulkExportZip(false) }
        )
    }

    // Folders & Tags Filter Strip (Premium Smart Organization)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = "Folders",
            tint = theme.accent,
            modifier = Modifier.size(15.dp)
        )
        availableFolders.forEach { folder ->
            val selected = selectedFolder.equals(folder, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) theme.accent else theme.surface)
                    .pressScaleClickable(
                        testTag = "library_folder_chip_${folder.lowercase()}",
                        onClick = { onSelectFolder(folder) }
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (folder == "All") "All Folders" else "📁 $folder",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) BentoTextBlack else theme.textSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        availableTags.filter { it != "All" }.forEach { tag ->
            val selected = selectedTag.equals(tag, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) theme.bentoSky else theme.surface)
                    .pressScaleClickable(
                        testTag = "library_tag_chip_${tag.lowercase()}",
                        onClick = { onSelectTag(if (selected) "All" else tag) }
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "#$tag",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) BentoTextBlack else theme.bentoSky
                )
            }
        }
    }

    // Bulk Action Bar when Bulk Select Mode is active
    if (isBulkSelectMode) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.elevated)
                .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState())
                .testTag("library_bulk_action_bar")
        ) {
            GlassButton(
                text = "All",
                onClick = onSelectAllDocs,
                testTag = "bulk_select_all_button"
            )
            GlassButton(
                text = "Move",
                icon = Icons.Default.Folder,
                onClick = onBulkMoveFolder,
                testTag = "bulk_move_folder_button"
            )
            GlassButton(
                text = "Merge",
                icon = Icons.Default.MergeType,
                onClick = onBulkMerge,
                testTag = "bulk_merge_button"
            )
            GlassButton(
                text = "Share ZIP",
                icon = Icons.Default.Share,
                onClick = { onBulkExportZip(false) },
                testTag = "bulk_share_zip_button"
            )
            GlassButton(
                text = "Save ZIP",
                icon = Icons.Default.Download,
                onClick = { onBulkExportZip(true) },
                testTag = "bulk_save_zip_button"
            )
            GlassButton(
                text = "Delete",
                icon = Icons.Default.DeleteOutline,
                onClick = onBulkDelete,
                testTag = "bulk_delete_button"
            )
        }
    }

    // Row with caption "N documents" and two fixed 44dp circular buttons (Sort dropdown and Scan)
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "${documents.size} ${if (documents.size == 1) "document" else "documents"}",
            style = type.caption.copy(fontWeight = FontWeight.Bold),
            color = theme.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .testTag("library_documents_count_caption")
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed 44dp Circular Button 1: Sort Dropdown (Newest, Oldest, Name A-Z, Pages)
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                        .pressScaleClickable(
                            testTag = "library_doc_sort_button",
                            onClick = { showSortDropdown = true }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort Documents",
                        tint = theme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSortDropdown,
                    onDismissRequest = { showSortDropdown = false }
                ) {
                    DocumentSortOrder.entries.forEach { order ->
                        val isSelected = selectedSort == order
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = theme.accent
                                    )
                                }
                            },
                            onClick = {
                                onSelectSort(order)
                                showSortDropdown = false
                            }
                        )
                    }
                }
            }

            // Fixed 44dp Circular Button 2: Scan
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(theme.bentoCoral)
                    .pressScaleClickable(
                        testTag = "library_scan_document_button",
                        onClick = onScanNewDocument
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = "Scan a document",
                    tint = BentoTextBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (documents.isEmpty()) {
        VaultEmptyCard(
            icon = Icons.Default.Description,
            accentColor = theme.bentoCoral,
            title = "No Documents Yet",
            description = "Scan receipts, IDs, bills, or multi-page documents with automatic edge detection, standard PDF page sizes, and offline OCR search.",
            buttonText = "Scan a document",
            buttonTestTag = "library_empty_scan_document_button",
            onAction = onScanNewDocument
        )
    } else {
        // 2-column grid of equal cards (3:4 thumbnail, one-line title, date & category dot, star, long-press menu)
        val rows = remember(documents) { documents.chunked(2) }
        var docBannerSlots = 0
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEachIndexed { rowIndex, rowDocs ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowDocs.forEach { doc ->
                        DocumentThumbnailCard(
                            doc = doc,
                            isBulkSelectMode = isBulkSelectMode,
                            isSelectedForBulk = doc.id in selectedDocIds,
                            onClick = { onOpenDocument(doc) },
                            onToggleFavorite = { onToggleFavorite(doc) },
                            onRename = { onRequestRename(doc) },
                            onShare = { onShareDocument(doc) },
                            onSaveToPhone = { onSaveToPhone(doc) },
                            onToggleLock = { onToggleLock(doc) },
                            onDelete = { onDeleteDocument(doc) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowDocs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                val itemsRendered = (rowIndex * 2) + rowDocs.size
                // Banner after every 6th item (reserving 1 slot for the end so max 2 ad slots per screen)
                if (itemsRendered % 6 == 0 && rowIndex < rows.lastIndex && docBannerSlots < 1) {
                    docBannerSlots++
                    InlineBannerAdSlot(placementTag = "documents_after_$itemsRendered")
                }
            }
        }
    }

    // Reserved banner slot at the end of the Documents list (max 2 ad slots per screen)
    InlineBannerAdSlot(placementTag = "documents_list_end")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentThumbnailCard(
    doc: DocumentRecord,
    isBulkSelectMode: Boolean = false,
    isSelectedForBulk: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onSaveToPhone: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.US) }
    var showLongPressMenu by remember { mutableStateOf(false) }

    val categoryColor = when (doc.category.uppercase(Locale.US)) {
        "ID" -> theme.bentoSky
        "RECEIPT" -> theme.bentoCoral
        "BILL" -> theme.bentoLavender
        "NOTES" -> theme.bentoLime
        else -> theme.accent
    }

    val thumbnailBitmap = remember(doc.thumbnailPath, doc.pageCount) {
        val file = File(doc.thumbnailPath)
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.surface)
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        FeedbackHelper.hapticTick(context)
                        showLongPressMenu = true
                    }
                )
                .testTag("doc_grid_card_${doc.id}")
        ) {
            // 3:4 Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(theme.elevated)
            ) {
                if (thumbnailBitmap != null && !doc.locked) {
                    Image(
                        bitmap = thumbnailBitmap.asImageBitmap(),
                        contentDescription = doc.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (doc.locked) Icons.Default.Lock else Icons.Default.Description,
                            contentDescription = doc.name,
                            tint = categoryColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Page count + Page size badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${doc.pageCount}p • ${doc.resolvedPageSize.label}",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1
                    )
                }

                // Star + Lock badges at TopEnd
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    if (doc.locked) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.72f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = theme.accent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f))
                            .pressScaleClickable(
                                testTag = "doc_star_${doc.id}",
                                onClick = onToggleFavorite
                            )
                    ) {
                        Icon(
                            imageVector = if (doc.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (doc.favorite) theme.accent else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Card footer: one-line title, date and category dot
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = doc.name,
                    style = type.body.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isBulkSelectMode) {
                        Icon(
                            imageVector = if (isSelectedForBulk) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Selected",
                            tint = if (isSelectedForBulk) theme.accent else theme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                        )
                    }
                    Text(
                        text = doc.category,
                        style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = categoryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "• ${dateFmt.format(Date(doc.createdAt))}",
                        style = type.caption,
                        color = theme.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (doc.folderName.isNotBlank() || doc.expiryEpochMs != null || doc.isPasswordProtected) {
                    val expiryBadge = doc.expiryEpochMs?.let {
                        DocumentSmartTools.ExpiryReminderScheduler.formatExpiryBadge(it)
                    }
                    Text(
                        text = listOfNotNull(
                            doc.folderName.takeIf { it.isNotBlank() }?.let { "📁 $it" },
                            if (doc.isPasswordProtected) "🔒 PDF" else null,
                            expiryBadge
                        ).joinToString(" • "),
                        style = type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = if (expiryBadge != null) theme.bentoCoral else theme.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Long-press menu: Rename, Share, Save to phone, Lock, Delete
        DropdownMenu(
            expanded = showLongPressMenu,
            onDismissRequest = { showLongPressMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = {
                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename")
                },
                onClick = {
                    showLongPressMenu = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                },
                onClick = {
                    showLongPressMenu = false
                    onShare()
                }
            )
            DropdownMenuItem(
                text = { Text("Save to phone") },
                leadingIcon = {
                    Icon(Icons.Default.Download, contentDescription = "Save to phone")
                },
                onClick = {
                    showLongPressMenu = false
                    onSaveToPhone()
                }
            )
            DropdownMenuItem(
                text = { Text(if (doc.locked) "Unlock" else "Lock") },
                leadingIcon = {
                    Icon(
                        imageVector = if (doc.locked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = if (doc.locked) "Unlock" else "Lock"
                    )
                },
                onClick = {
                    showLongPressMenu = false
                    onToggleLock()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = theme.bentoCoral) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = theme.bentoCoral
                    )
                },
                onClick = {
                    showLongPressMenu = false
                    onDelete()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocumentViewerScreen(
    doc: DocumentRecord,
    allDocuments: List<DocumentRecord>,
    scanViewModel: ScanViewModel,
    isPro: Boolean,
    featureGate: FeatureGate? = null,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val scope = rememberCoroutineScope()

    val pagePaths = remember(doc.pagePathsJoined) { doc.pagePaths }
    val pageCount = pagePaths.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pageCount }
    )

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var showRenameSheet by remember { mutableStateOf(false) }
    var showMergeSheet by remember { mutableStateOf(false) }
    var showFiltersSheet by remember { mutableStateOf(false) }
    var showPageSettingsSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showOrganizeExpirySheet by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }
    var showReceiptParserSheet by remember { mutableStateOf(false) }
    var showBusinessCardParserSheet by remember { mutableStateOf(false) }

    fun requireDocPack(feature: DocumentPackFeature, onAllowed: () -> Unit) {
        if (isPro || featureGate == null) {
            onAllowed()
        } else {
            featureGate.requireDocumentPack(feature, onAllowed)
        }
    }

    // Per-document Page Size, Fit/Fill, Margin, Quality, Filter & Sliders state (changeable & re-exportable)
    var selectedPageSize by remember(doc.id, doc.pageSizeId) { mutableStateOf(doc.resolvedPageSize) }
    var selectedFitMode by remember(doc.id, doc.fitModeId) { mutableStateOf(doc.resolvedFitMode) }
    var selectedMarginMode by remember(doc.id, doc.marginModeId) { mutableStateOf(doc.resolvedMarginMode) }
    var selectedQualityDpi by remember(doc.id, doc.qualityDpiId) { mutableStateOf(doc.resolvedQualityDpi) }
    var selectedFilterMode by remember(doc.id, doc.filterMode) {
        mutableStateOf(doc.filterMode.ifBlank { "Original Color" })
    }
    var brightnessDelta by remember(doc.id, doc.brightnessDelta) { mutableFloatStateOf(doc.brightnessDelta) }
    var contrastScale by remember(doc.id, doc.contrastScale) { mutableFloatStateOf(doc.contrastScale) }
    var sharpenAmount by remember(doc.id, doc.sharpenAmount) { mutableFloatStateOf(doc.sharpenAmount) }
    var batchApplyAllPages by remember { mutableStateOf(true) }
    var filterCompareSplitFraction by remember { mutableFloatStateOf(0.5f) }

    val firstDimensions = remember(pagePaths) {
        val firstPath = pagePaths.firstOrNull()
        if (firstPath != null && File(firstPath).exists()) {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(firstPath, opts)
            (opts.outWidth.coerceAtLeast(800)) to (opts.outHeight.coerceAtLeast(1120))
        } else {
            800 to 1120
        }
    }

    val detectedPageSize = remember(firstDimensions) {
        PdfPageSize.detectClosestStandardSize(firstDimensions.first, firstDimensions.second)
    }

    val estimatedBytes = remember(pagePaths, selectedPageSize, selectedQualityDpi) {
        val pathsToEstimate = doc.originalPagePaths.ifEmpty { pagePaths }
        ScanRepository.estimatePdfSizeBytesFromFiles(pathsToEstimate, selectedPageSize, selectedQualityDpi)
    }
    val estimatedSizeLabel = remember(estimatedBytes) {
        ScanRepository.formatFileSize(estimatedBytes)
    }

    LaunchedEffect(pagerState.currentPage) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    val addPageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = ScanRepository.decodeUriWithExifRotation(context, uri)
            if (bmp != null) {
                scanViewModel.addPageToDocument(context, doc, bmp)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("document_viewer_fullscreen")
    ) {
        // 1. TOP BAR: Back button, Title that wraps up to 2 lines (never single-line "Scan 2026-1..."), "Page 1 / N" chip, and three-dot menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                hazeState = hazeState,
                onClick = onClose,
                testTag = "top_bar_back_button"
            )

            Text(
                text = doc.name,
                style = type.title.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                ),
                color = theme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("doc_viewer_title")
            )

            // "Page 1 / N" chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.surface)
                    .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("doc_viewer_page_chip")
            ) {
                Text(
                    text = "Page ${pagerState.currentPage + 1} / $pageCount",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Three-dot menu
            Box {
                GlassIconButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "More Document Options",
                    hazeState = hazeState,
                    onClick = { showMoreMenu = true },
                    testTag = "doc_viewer_three_dot_button"
                )

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            showRenameSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Folders, Tags & Expiry Reminder") },
                        leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            showOrganizeExpirySheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Receipt & Expense OCR") },
                        leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            requireDocPack(DocumentPackFeature.SMART_RECEIPT_EXPENSE) {
                                showReceiptParserSheet = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Business Card → Contacts") },
                        leadingIcon = { Icon(Icons.Default.ContactPage, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            requireDocPack(DocumentPackFeature.SMART_BUSINESS_CARD) {
                                showBusinessCardParserSheet = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Password Protect / Unlock PDF") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            requireDocPack(DocumentPackFeature.PDF_PASSWORD_PROTECT) {
                                showPasswordSheet = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isPro) "Merge" else "Merge (Pro / 1h)") },
                        leadingIcon = { Icon(Icons.Default.MergeType, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            requireDocPack(DocumentPackFeature.PDF_UNLIMITED_MERGE) {
                                showMergeSheet = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (doc.locked) "Unlock" else "Lock") },
                        leadingIcon = {
                            Icon(
                                if (doc.locked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            if (!isPro && !doc.locked) {
                                onOpenPaywall()
                            } else {
                                authenticateWithBiometrics(
                                    context = context,
                                    title = if (doc.locked) "Unlock Document" else "Lock this Document",
                                    subtitle = "Use fingerprint, face, or device PIN",
                                    onAuthenticated = {
                                        scanViewModel.setDocumentLocked(doc, !doc.locked)
                                    },
                                    onInfoMessage = { msg -> scanViewModel.showToast(msg) }
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Page size (${selectedPageSize.label})") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            showPageSettingsSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export quality (${selectedQualityDpi.label})") },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            showPageSettingsSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = theme.bentoCoral) },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = theme.bentoCoral)
                        },
                        onClick = {
                            showMoreMenu = false
                            scanViewModel.deleteDocumentWithUndo(doc)
                        }
                    )
                }
            }
        }

        // 2. HORIZONTAL PAGER WITH PINCH ZOOM (weight(1f)): each page shown at the document's page ratio on a white sheet with a soft shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIdx ->
                val path = pagePaths.getOrNull(pageIdx)
                val bmp = remember(path, doc.filterMode, doc.brightnessDelta, doc.contrastScale, doc.sharpenAmount) {
                    if (path != null && File(path).exists()) {
                        BitmapFactory.decodeFile(path)
                    } else {
                        ScanRepository.generateSampleDocumentBitmap(
                            title = doc.name,
                            category = doc.category,
                            filterMode = doc.filterMode,
                            pageNumber = pageIdx + 1
                        )
                    }
                }

                val srcW = bmp?.width ?: 800
                val srcH = bmp?.height ?: 1120
                val (pageWPt, pageHPt) = remember(selectedPageSize, srcW, srcH) {
                    selectedPageSize.resolvePageDimensionsPt(srcW, srcH)
                }
                val pageAspectRatio = remember(pageWPt, pageHPt) {
                    (pageWPt.toFloat() / pageHPt.toFloat().coerceAtLeast(1f)).coerceIn(0.38f, 2.2f)
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageIdx) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                zoomScale = newScale
                                panOffset = if (newScale > 1f) {
                                    Offset(
                                        x = (panOffset.x + pan.x).coerceIn(-600f, 600f),
                                        y = (panOffset.y + pan.y).coerceIn(-600f, 600f)
                                    )
                                } else {
                                    Offset.Zero
                                }
                            }
                        }
                ) {
                    // White sheet at the document's page ratio with a soft shadow
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxHeight(0.96f)
                            .aspectRatio(pageAspectRatio, matchHeightConstraintsFirst = true)
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = panOffset.x,
                                translationY = panOffset.y
                            )
                            .shadow(18.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(
                                when {
                                    selectedFitMode == PdfFitMode.FILL -> 0.dp
                                    selectedMarginMode == PdfMarginMode.NONE -> 0.dp
                                    selectedMarginMode == PdfMarginMode.NORMAL_20MM -> 16.dp
                                    else -> 8.dp
                                }
                            )
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${pageIdx + 1}",
                                contentScale = if (selectedFitMode == PdfFitMode.FILL) ContentScale.Crop else ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            if (zoomScale > 1.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.accent)
                        .pressScaleClickable(onClick = {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        })
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Reset ${String.format(Locale.US, "%.1fx", zoomScale)}",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = BentoTextBlack
                    )
                }
            }
        }

        // 3. GLASS PAGE STRIP (thumbnails, add, rotate, delete)
        GlassSurface(
            hazeState = hazeState,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("doc_viewer_page_strip")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Scrollable page thumbnails
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(pagePaths) { idx, path ->
                        val isCurrent = pagerState.currentPage == idx
                        val thumbBmp = remember(path) {
                            if (File(path).exists()) BitmapFactory.decodeFile(path) else null
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) theme.accent else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .pressScaleClickable(
                                    testTag = "doc_strip_thumb_$idx",
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(idx) }
                                    }
                                )
                        ) {
                            if (thumbBmp != null) {
                                Image(
                                    bitmap = thumbBmp.asImageBitmap(),
                                    contentDescription = "Thumbnail ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) theme.accent else Color.White
                                )
                            }
                        }
                    }
                }

                // Strip actions: Add, Rotate, Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StripIconActionButton(
                        icon = Icons.Default.AddPhotoAlternate,
                        label = "Add",
                        tint = theme.accent,
                        testTag = "doc_add_page_button",
                        onClick = {
                            addPageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    StripIconActionButton(
                        icon = Icons.Default.RotateRight,
                        label = "Rotate",
                        tint = theme.bentoSky,
                        testTag = "doc_rotate_page_button",
                        onClick = {
                            FeedbackHelper.hapticTick(context)
                            scanViewModel.rotateDocumentPage(context, doc, pagerState.currentPage)
                        }
                    )
                    StripIconActionButton(
                        icon = Icons.Default.DeleteOutline,
                        label = "Delete",
                        tint = theme.bentoCoral,
                        testTag = "doc_delete_page_button",
                        onClick = {
                            FeedbackHelper.hapticTick(context)
                            scanViewModel.deleteDocumentPage(context, doc, pagerState.currentPage)
                        }
                    )
                }
            }
        }

        if (!isPro) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                QuotaAllowanceChip(
                    quotaType = QuotaType.OCR,
                    isProUnlocked = isPro,
                    onClick = { featureGate?.openQuotaUnlockSheet(QuotaType.OCR) }
                )
                QuotaAllowanceChip(
                    quotaType = QuotaType.PDF_EXPORT,
                    isProUnlocked = isPro,
                    onClick = { featureGate?.openQuotaUnlockSheet(QuotaType.PDF_EXPORT) }
                )
            }
        }

        // 4. BOTTOM GLASS ACTION BAR with icon and label: Share, Text (OCR), Filters, Save, More
        GlassSurface(
            hazeState = hazeState,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("doc_viewer_bottom_action_bar")
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                ViewerBottomBarItem(
                    icon = Icons.Default.Share,
                    label = "Share",
                    tint = theme.bentoSky,
                    testTag = "doc_share_pdf_button",
                    onClick = {
                        val runShare: () -> Unit = {
                            scanViewModel.shareDocumentPdf(context, doc) { chooser ->
                                context.startActivity(chooser)
                            }
                        }
                        if (isPro || featureGate == null) {
                            runShare()
                        } else {
                            featureGate.requireQuotaOrUnlock(
                                quotaType = QuotaType.PDF_EXPORT,
                                onAllowed = runShare
                            )
                        }
                    }
                )
                ViewerBottomBarItem(
                    icon = Icons.Default.TextFields,
                    label = "Text",
                    tint = if (isPro) theme.accent else ProGold,
                    testTag = "doc_extract_ocr_button",
                    onClick = {
                        val runOcr: () -> Unit = {
                            scanViewModel.launchExtractOcrSheet(
                                context = context,
                                doc = doc,
                                onePagePreviewOnly = !isPro,
                                pageIndex = pagerState.currentPage
                            )
                        }
                        if (isPro || featureGate == null) {
                            runOcr()
                        } else {
                            featureGate.requireQuotaOrUnlock(
                                quotaType = QuotaType.OCR,
                                onAllowed = runOcr
                            )
                        }
                    }
                )
                ViewerBottomBarItem(
                    icon = Icons.Default.AutoFixHigh,
                    label = "Filters",
                    tint = theme.bentoLime,
                    testTag = "doc_filters_button",
                    onClick = {
                        showFiltersSheet = true
                    }
                )
                ViewerBottomBarItem(
                    icon = Icons.Default.Download,
                    label = "Save",
                    tint = theme.bentoCoral,
                    testTag = "doc_save_to_phone_button",
                    onClick = {
                        val runSave: () -> Unit = {
                            FeedbackHelper.vibrateTick(context, 30L, 180)
                            scanViewModel.saveDocumentToPhone(context, doc)
                        }
                        if (isPro || featureGate == null) {
                            runSave()
                        } else {
                            featureGate.requireQuotaOrUnlock(
                                quotaType = QuotaType.PDF_EXPORT,
                                onAllowed = runSave
                            )
                        }
                    }
                )
                ViewerBottomBarItem(
                    icon = Icons.Default.MoreVert,
                    label = "More",
                    tint = theme.textPrimary,
                    testTag = "doc_more_actions_button",
                    onClick = {
                        showPageSettingsSheet = true
                    }
                )
            }
        }
    }

    // Filters Glass Sheet: Original Color (Free), Magic Color, B&W adaptive threshold, Grayscale, Whiteboard, Shadow removal + Brightness/Contrast/Sharpen sliders + Batch apply
    if (showFiltersSheet) {
        GlassSheet(
            onDismissRequest = { showFiltersSheet = false },
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
                    Text(
                        text = "Scan Filters & Adjustments",
                        style = type.title,
                        color = theme.textPrimary
                    )
                    if (!isPro) {
                        ProBadge()
                    }
                }
                Text(
                    text = "Original Color is free. Tap any Pro filter below to try Before / After comparison before unlocking.",
                    style = type.caption,
                    color = theme.textSecondary
                )

                // Try Before You Buy: Before / After Compare Slider
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ProGold.copy(alpha = 0.10f))
                        .border(1.dp, ProGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("doc_filter_before_after_slider_box")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "BEFORE (Original) ⇄ AFTER ($selectedFilterMode)",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = ProGold
                        )
                        Text(
                            text = "${(filterCompareSplitFraction * 100).toInt()}%",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                    }
                    Slider(
                        value = filterCompareSplitFraction,
                        onValueChange = { filterCompareSplitFraction = it },
                        valueRange = 0.05f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = ProGold,
                            activeTrackColor = ProGold
                        ),
                        modifier = Modifier.testTag("doc_filter_compare_slider")
                    )
                }

                ProScanFilterMode.entries.forEach { mode ->
                    val isSelected = selectedFilterMode.equals(mode.label, ignoreCase = true)
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) theme.accent else theme.surface)
                            .border(
                                1.dp,
                                if (isSelected) theme.accent else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(16.dp)
                            )
                            .pressScaleClickable(
                                testTag = "doc_filter_option_${mode.label.lowercase().replace(" ", "_")}",
                                onClick = {
                                    selectedFilterMode = mode.label
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.label,
                                    style = type.body.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) BentoTextBlack else theme.textPrimary
                                )
                                if (mode.isPremium && !isPro) {
                                    ProBadge()
                                }
                            }
                            Text(
                                text = mode.subtitle,
                                style = type.caption,
                                color = if (isSelected) BentoTextBlack.copy(alpha = 0.75f) else theme.textSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = BentoTextBlack
                            )
                        }
                    }
                }

                // Brightness / Contrast / Sharpen Sliders
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Brightness (${brightnessDelta.toInt()})",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "Contrast (${String.format(Locale.US, "%.2fx", contrastScale)}) • Sharpen (${(sharpenAmount * 100).toInt()}%)",
                            style = type.caption,
                            color = theme.accent
                        )
                    }

                    Slider(
                        value = brightnessDelta,
                        onValueChange = { brightnessDelta = it },
                        valueRange = -80f..80f,
                        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                    )

                    Text(
                        text = "Contrast (${String.format(Locale.US, "%.2fx", contrastScale)})",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Slider(
                        value = contrastScale,
                        onValueChange = { contrastScale = it },
                        valueRange = 0.6f..1.8f,
                        colors = SliderDefaults.colors(thumbColor = theme.bentoSky, activeTrackColor = theme.bentoSky)
                    )

                    Text(
                        text = "Sharpen (${(sharpenAmount * 100).toInt()}%)",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Slider(
                        value = sharpenAmount,
                        onValueChange = { sharpenAmount = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = theme.bentoLime, activeTrackColor = theme.bentoLime)
                    )

                    // Batch apply toggle
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.elevated)
                            .pressScaleClickable(
                                testTag = "doc_filter_batch_toggle",
                                onClick = { batchApplyAllPages = !batchApplyAllPages }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (batchApplyAllPages) {
                                "Batch apply to all ${pagePaths.size} pages"
                            } else {
                                "Apply to current page only (Page ${pagerState.currentPage + 1})"
                            },
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                        Icon(
                            imageVector = if (batchApplyAllPages) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Batch apply",
                            tint = theme.accent
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Reset",
                        onClick = {
                            selectedFilterMode = "Original Color"
                            brightnessDelta = 0f
                            contrastScale = 1f
                            sharpenAmount = 0f
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = if (batchApplyAllPages) "Apply to All Pages" else "Apply to Page ${pagerState.currentPage + 1}",
                        accentFill = theme.accent,
                        onClick = {
                            val needsPro = !selectedFilterMode.equals("Original Color", ignoreCase = true) ||
                                kotlin.math.abs(brightnessDelta) > 0.5f ||
                                kotlin.math.abs(contrastScale - 1f) > 0.02f ||
                                sharpenAmount > 0.02f
                            val runApply = {
                                scanViewModel.applySmartFilterAndSliders(
                                    context = context,
                                    doc = doc,
                                    filterMode = selectedFilterMode,
                                    brightnessDelta = brightnessDelta,
                                    contrastScale = contrastScale,
                                    sharpenAmount = sharpenAmount,
                                    batchAllPages = batchApplyAllPages,
                                    targetPageIndex = pagerState.currentPage
                                )
                                showFiltersSheet = false
                            }
                            if (needsPro && !isPro) {
                                requireDocPack(DocumentPackFeature.SMART_SCAN_FILTERS, runApply)
                            } else {
                                runApply()
                            }
                        },
                        modifier = Modifier.weight(1.6f),
                        testTag = "doc_apply_filters_button"
                    )
                }
            }
        }
    }

    // More / Page Size & Export Quality Glass Sheet
    if (showPageSettingsSheet) {
        GlassSheet(
            onDismissRequest = { showPageSettingsSheet = false },
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
                    Text(
                        text = "More & Smart Document Tools",
                        style = type.title,
                        color = theme.textPrimary
                    )
                    if (detectedPageSize != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.accent.copy(alpha = 0.16f))
                                .pressScaleClickable { selectedPageSize = detectedPageSize }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Looks like ${detectedPageSize.label}",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.accent
                            )
                        }
                    }
                }

                // Quick actions row: Rename, Merge, Lock, Delete, Reorder + Smart Tools
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Rename",
                        icon = Icons.Default.DriveFileRenameOutline,
                        onClick = {
                            showPageSettingsSheet = false
                            showRenameSheet = true
                        },
                        testTag = "doc_rename_button"
                    )
                    GlassButton(
                        text = if (isPro) "Folder, Tags & Expiry" else "Folder & Expiry • PRO",
                        icon = Icons.Default.NotificationsActive,
                        accentFill = if (isPro) null else ProGold.copy(alpha = 0.22f),
                        textColor = if (isPro) theme.textPrimary else ProGold,
                        onClick = {
                            showPageSettingsSheet = false
                            showOrganizeExpirySheet = true
                        },
                        testTag = "doc_organize_expiry_button"
                    )
                    GlassButton(
                        text = if (isPro) "Receipt OCR" else "Receipt OCR • PRO",
                        icon = Icons.Default.ReceiptLong,
                        accentFill = if (isPro) null else ProGold.copy(alpha = 0.22f),
                        textColor = if (isPro) theme.textPrimary else ProGold,
                        onClick = {
                            showPageSettingsSheet = false
                            requireDocPack(DocumentPackFeature.SMART_RECEIPT_EXPENSE) {
                                showReceiptParserSheet = true
                            }
                        },
                        testTag = "doc_receipt_ocr_button"
                    )
                    GlassButton(
                        text = if (isPro) "Business Card" else "Business Card • PRO",
                        icon = Icons.Default.ContactPage,
                        accentFill = if (isPro) null else ProGold.copy(alpha = 0.22f),
                        textColor = if (isPro) theme.textPrimary else ProGold,
                        onClick = {
                            showPageSettingsSheet = false
                            requireDocPack(DocumentPackFeature.SMART_BUSINESS_CARD) {
                                showBusinessCardParserSheet = true
                            }
                        },
                        testTag = "doc_business_card_button"
                    )
                    GlassButton(
                        text = if (isPro) "PDF Password" else "PDF Password • PRO",
                        icon = Icons.Default.Lock,
                        accentFill = if (isPro) null else ProGold.copy(alpha = 0.22f),
                        textColor = if (isPro) theme.textPrimary else ProGold,
                        onClick = {
                            showPageSettingsSheet = false
                            requireDocPack(DocumentPackFeature.PDF_PASSWORD_PROTECT) {
                                showPasswordSheet = true
                            }
                        },
                        testTag = "doc_pdf_password_button"
                    )
                    GlassButton(
                        text = if (isPro) "Merge" else "Merge • PRO",
                        icon = Icons.Default.MergeType,
                        accentFill = if (isPro) null else ProGold.copy(alpha = 0.22f),
                        textColor = if (isPro) theme.textPrimary else ProGold,
                        onClick = {
                            showPageSettingsSheet = false
                            requireDocPack(DocumentPackFeature.PDF_UNLIMITED_MERGE) {
                                showMergeSheet = true
                            }
                        },
                        testTag = "doc_merge_button"
                    )
                    GlassButton(
                        text = if (doc.locked) "Unlock" else if (isPro) "Lock" else "Lock • PRO",
                        icon = if (doc.locked) Icons.Default.LockOpen else Icons.Default.Lock,
                        accentFill = if (isPro || doc.locked) null else ProGold.copy(alpha = 0.22f),
                        textColor = if (isPro || doc.locked) theme.textPrimary else ProGold,
                        onClick = {
                            showPageSettingsSheet = false
                            if (!isPro && !doc.locked) {
                                onOpenPaywall()
                            } else {
                                authenticateWithBiometrics(
                                    context = context,
                                    title = if (doc.locked) "Unlock Document" else "Lock this Document",
                                    subtitle = "Use fingerprint, face, or device PIN",
                                    onAuthenticated = {
                                        scanViewModel.setDocumentLocked(doc, !doc.locked)
                                    },
                                    onInfoMessage = { msg -> scanViewModel.showToast(msg) }
                                )
                            }
                        },
                        testTag = "doc_lock_toggle_button"
                    )
                    GlassButton(
                        text = "Move Page ◀",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        onClick = {
                            val cur = pagerState.currentPage
                            if (cur > 0) scanViewModel.reorderDocumentPage(context, doc, cur, cur - 1)
                        },
                        testTag = "doc_page_move_left"
                    )
                    GlassButton(
                        text = "Move Page ▶",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        onClick = {
                            val cur = pagerState.currentPage
                            if (cur < pagePaths.lastIndex) scanViewModel.reorderDocumentPage(context, doc, cur, cur + 1)
                        },
                        testTag = "doc_page_move_right"
                    )
                    GlassButton(
                        text = "Delete Document",
                        icon = Icons.Default.DeleteOutline,
                        onClick = {
                            showPageSettingsSheet = false
                            scanViewModel.deleteDocumentWithUndo(doc)
                        },
                        testTag = "doc_delete_button"
                    )
                }

                DocumentPageSizePreviewCard(
                    sourceWidth = firstDimensions.first,
                    sourceHeight = firstDimensions.second,
                    pageSize = selectedPageSize,
                    fitMode = selectedFitMode,
                    marginMode = selectedMarginMode,
                    qualityDpi = selectedQualityDpi,
                    estimatedSizeLabel = estimatedSizeLabel
                )

                Text(
                    text = "PAGE SIZE",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) theme.accent else theme.elevated)
                                .pressScaleClickable(
                                    testTag = "viewer_page_size_${sizeOption.id}",
                                    onClick = { selectedPageSize = sizeOption }
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${sizeOption.label} (${sizeOption.subtitle})",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PdfFitMode.entries.forEach { mode ->
                        val isSelected = selectedFitMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) theme.accent else theme.elevated)
                                .pressScaleClickable { selectedFitMode = mode }
                                .padding(vertical = 7.dp),
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) theme.bentoSky else theme.elevated)
                                    .pressScaleClickable { selectedMarginMode = margin }
                                    .padding(vertical = 6.dp),
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

                Text(
                    text = "EXPORT QUALITY",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
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
                                .background(if (isSelected) theme.accent else theme.elevated)
                                .pressScaleClickable { selectedQualityDpi = dpiOption }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = dpiOption.label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }

                GlassButton(
                    text = "Re-export PDF (${selectedPageSize.label} • ~$estimatedSizeLabel)",
                    icon = Icons.Default.PictureAsPdf,
                    accentFill = theme.accent,
                    onClick = {
                        FeedbackHelper.vibrateTick(context, 30L, 180)
                        scanViewModel.reExportDocumentSettings(
                            context = context,
                            doc = doc,
                            pageSize = selectedPageSize,
                            fitMode = selectedFitMode,
                            marginMode = selectedMarginMode,
                            qualityDpi = selectedQualityDpi,
                            filterMode = selectedFilterMode
                        )
                        showPageSettingsSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "doc_reexport_pdf_button"
                )
            }
        }
    }

    if (showRenameSheet) {
        RenameDocumentGlassSheet(
            currentName = doc.name,
            hazeState = hazeState,
            onRename = { newName ->
                scanViewModel.renameDocument(doc.id, newName)
                showRenameSheet = false
            },
            onDismiss = { showRenameSheet = false }
        )
    }

    if (showMergeSheet) {
        val otherDocs = allDocuments.filter { it.id != doc.id }
        MergeDocumentGlassSheet(
            targetDoc = doc,
            candidateDocs = otherDocs,
            hazeState = hazeState,
            onSelectMergeSource = { sourceDoc ->
                scanViewModel.mergeWithDocument(context, doc, sourceDoc)
                showMergeSheet = false
            },
            onDismiss = { showMergeSheet = false }
        )
    }

    if (showOrganizeExpirySheet) {
        DocumentOrganizeExpirySheet(
            doc = doc,
            isPro = isPro,
            hazeState = hazeState,
            onRequirePackFeature = ::requireDocPack,
            onSave = { folder, tags, expiryMs ->
                scanViewModel.saveDocumentOrganizationAndExpiry(
                    context = context,
                    doc = doc,
                    folderName = folder,
                    tagsCsv = tags,
                    expiryEpochMs = expiryMs
                )
                showOrganizeExpirySheet = false
            },
            onDismiss = { showOrganizeExpirySheet = false }
        )
    }

    if (showPasswordSheet) {
        DocumentPasswordProtectSheet(
            doc = doc,
            hazeState = hazeState,
            onProtect = { pwd ->
                scanViewModel.protectDocumentWithPassword(context, doc, pwd)
                showPasswordSheet = false
            },
            onUnlock = { pwd ->
                scanViewModel.unlockPasswordProtectedDocument(context, doc, pwd)
                showPasswordSheet = false
            },
            onDismiss = { showPasswordSheet = false }
        )
    }

    if (showReceiptParserSheet) {
        ReceiptExpenseCenterGlassSheet(
            documents = allDocuments,
            initialDoc = doc,
            scanViewModel = scanViewModel,
            hazeState = hazeState,
            onDismiss = { showReceiptParserSheet = false }
        )
    }

    if (showBusinessCardParserSheet) {
        BusinessCardCenterGlassSheet(
            documents = allDocuments,
            initialDoc = doc,
            scanViewModel = scanViewModel,
            hazeState = hazeState,
            onDismiss = { showBusinessCardParserSheet = false }
        )
    }
}

@Composable
private fun StripIconActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(theme.surface.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .pressScaleClickable(testTag = testTag, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun ViewerBottomBarItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .pressScaleClickable(testTag = testTag, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OcrExtractGlassSheet(
    documentName: String,
    ocrText: String,
    isExtracting: Boolean,
    isOnePagePreview: Boolean,
    hazeState: HazeState?,
    onUnlockFullOcr: () -> Unit,
    onRequireOcrAction: (() -> Unit) -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val scope = rememberCoroutineScope()

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var selectedTargetLang by remember { mutableStateOf("es") }
    var isTranslating by remember { mutableStateOf(false) }
    var translationStatus by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.US
            }
        }
        ttsEngine = instance
        onDispose {
            instance?.stop()
            instance?.shutdown()
        }
    }

    GlassSheet(
        onDismissRequest = {
            ttsEngine?.stop()
            onDismiss()
        },
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
                Text(
                    text = if (isOnePagePreview) "1-Page OCR Preview" else "Extracted Text (On-Device OCR)",
                    style = type.title,
                    color = theme.textPrimary
                )
                if (isOnePagePreview) {
                    ProBadge()
                }
            }

            if (isOnePagePreview) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ProGold.copy(alpha = 0.14f))
                        .proBorder(RoundedCornerShape(16.dp))
                        .pressScaleClickable(
                            testTag = "ocr_unlock_full_button",
                            onClick = onUnlockFullOcr
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Free 1-Page OCR Preview Active",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = ProGold
                        )
                        Text(
                            text = "Tap to unlock full multi-page OCR, TXT export, Read Aloud (TTS) & On-Device Translation.",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlock Full OCR",
                        tint = ProGold
                    )
                }
            } else {
                Text(
                    text = if (isExtracting) {
                        "Recognizing text in \"$documentName\"..."
                    } else {
                        "Copy, share as .TXT, read aloud, or translate offline."
                    },
                    style = type.caption,
                    color = theme.accent
                )
            }

            OutlinedTextField(
                value = ocrText,
                onValueChange = onTextChange,
                label = { Text("Recognized Document Text") },
                minLines = 5,
                maxLines = 10,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Primary actions: Copy, Share as TXT, Read Aloud (TTS), Save
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassButton(
                    text = "Copy",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cb?.setPrimaryClip(ClipData.newPlainText("OCR Text", ocrText))
                        FeedbackHelper.vibrateTick(context)
                    },
                    testTag = "ocr_copy_button"
                )
                GlassButton(
                    text = "Share as TXT",
                    icon = Icons.Default.Share,
                    onClick = {
                        onRequireOcrAction {
                            val chooser = DocumentSmartTools.createShareTxtIntent(context, documentName, ocrText)
                            if (chooser != null) {
                                context.startActivity(chooser)
                            }
                        }
                    },
                    testTag = "ocr_share_txt_button"
                )
                GlassButton(
                    text = if (isSpeaking) "Stop Reading" else "Read Aloud",
                    icon = Icons.Default.RecordVoiceOver,
                    accentFill = if (isSpeaking) theme.bentoCoral else null,
                    onClick = {
                        onRequireOcrAction {
                            val engine = ttsEngine
                            if (engine != null) {
                                if (isSpeaking) {
                                    engine.stop()
                                    isSpeaking = false
                                } else {
                                    isSpeaking = true
                                    engine.speak(
                                        ocrText.take(3900),
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "dyne_ocr_tts"
                                    )
                                }
                            }
                        }
                    },
                    testTag = "ocr_tts_read_aloud_button"
                )
                GlassButton(
                    text = "Save Text",
                    accentFill = theme.accent,
                    onClick = onSave,
                    testTag = "ocr_save_button"
                )
            }

            // On-Device ML Kit Translation row (downloads model on first use; skips gracefully if unavailable)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Translate",
                            tint = theme.bentoSky,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Offline Translation",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                    }
                    if (isOnePagePreview) {
                        ProBadge()
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DocumentSmartTools.supportedTranslationLanguages) { (code, label) ->
                        val selected = selectedTargetLang == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) theme.bentoSky else theme.elevated)
                                .pressScaleClickable(
                                    testTag = "ocr_translate_lang_$code",
                                    onClick = { selectedTargetLang = code }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) BentoTextBlack else theme.textSecondary
                            )
                        }
                    }
                }

                if (translationStatus != null) {
                    Text(
                        text = translationStatus!!,
                        style = type.caption,
                        color = theme.bentoSky
                    )
                }

                GlassButton(
                    text = if (isTranslating) "Downloading Model & Translating..." else "Translate OCR Text",
                    icon = Icons.Default.Translate,
                    accentFill = theme.bentoSky,
                    onClick = {
                        if (!isTranslating) {
                            onRequireOcrAction {
                                isTranslating = true
                                translationStatus = "Checking on-device language model (downloads on first use)..."
                                scope.launch {
                                    val result = DocumentSmartTools.translateTextOnDevice(
                                        text = ocrText,
                                        sourceLangCode = "en",
                                        targetLangCode = selectedTargetLang
                                    )
                                    isTranslating = false
                                    translationStatus = result.statusMessage
                                    if (result.success) {
                                        onTextChange(result.translatedText)
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "ocr_translate_execute_button"
                )
            }
        }
    }
}

@Composable
private fun SmartLibraryChip(
    icon: ImageVector,
    label: String,
    isProUnlocked: Boolean,
    isActive: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) theme.accent else theme.surface)
            .border(
                1.dp,
                if (isActive) theme.accent else if (!isProUnlocked) ProGold.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .pressScaleClickable(testTag = testTag, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) BentoTextBlack else if (isProUnlocked) theme.accent else ProGold,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = type.caption.copy(fontWeight = FontWeight.Bold),
            color = if (isActive) BentoTextBlack else theme.textPrimary,
            maxLines = 1,
            softWrap = false
        )
        if (!isProUnlocked) {
            ProBadge()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiptExpenseCenterGlassSheet(
    documents: List<DocumentRecord>,
    initialDoc: DocumentRecord? = null,
    scanViewModel: ScanViewModel,
    hazeState: HazeState?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val candidateDocs = remember(documents) {
        documents.sortedByDescending { it.timestamp }
    }
    var selectedDoc by remember(candidateDocs, initialDoc) {
        mutableStateOf(
            initialDoc ?: candidateDocs.firstOrNull {
                it.category.equals("Receipt", ignoreCase = true) ||
                    it.category.equals("Bill", ignoreCase = true) ||
                    it.receiptTotalAmount > 0.0
            } ?: candidateDocs.firstOrNull()
        )
    }

    val initialParsed = remember(selectedDoc) {
        val d = selectedDoc
        if (d == null) {
            ReceiptParsedData("Merchant", "2026-10-27", 0.0, "Food & Dining")
        } else if (d.receiptTotalAmount > 0.0 || d.merchantName.isNotBlank()) {
            val base = DocumentSmartTools.parseReceiptOcr(d.ocrText, d.name)
            ReceiptParsedData(
                merchant = d.merchantName.ifBlank { base.merchant },
                dateIso = d.receiptDateIso.ifBlank { base.dateIso },
                totalAmount = if (d.receiptTotalAmount > 0.0) d.receiptTotalAmount else base.totalAmount,
                suggestedCategory = d.expenseCategory.ifBlank { base.suggestedCategory }
            )
        } else {
            DocumentSmartTools.parseReceiptOcr(d.ocrText, d.name)
        }
    }

    var merchantDraft by remember(initialParsed) { mutableStateOf(initialParsed.merchant) }
    var dateDraft by remember(initialParsed) { mutableStateOf(initialParsed.dateIso) }
    var totalDraft by remember(initialParsed) {
        mutableStateOf(
            if (initialParsed.totalAmount > 0.0) String.format(Locale.US, "%.2f", initialParsed.totalAmount) else ""
        )
    }
    var categoryDraft by remember(initialParsed) { mutableStateOf(initialParsed.suggestedCategory) }

    val monthlyBars = remember(documents, merchantDraft, totalDraft) {
        DocumentSmartTools.computeMonthlyExpenseTotals(documents)
    }

    GlassSheet(onDismissRequest = onDismiss, hazeState = hazeState) {
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
                        text = "Receipt & Expense Tracker",
                        style = type.title,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "OCR parses merchant, date & total • Monthly bar chart • CSV export",
                        style = type.caption,
                        color = theme.accent
                    )
                }
                ProBadge()
            }

            // Monthly Totals Bar Chart on Canvas
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                val grandTotal = monthlyBars.sumOf { it.total }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "6-MONTH EXPENSE TOTALS",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    Text(
                        text = String.format(Locale.US, "$%.2f", grandTotal),
                        style = type.body.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.accent
                    )
                }

                val maxBarVal = (monthlyBars.maxOfOrNull { it.total } ?: 1.0).coerceAtLeast(1.0)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("receipt_monthly_bar_chart")
                ) {
                    val count = monthlyBars.size.coerceAtLeast(1)
                    val slotW = size.width / count
                    val barW = (slotW * 0.52f).coerceAtLeast(12f)
                    val maxBarH = size.height - 22.dp.toPx()

                    monthlyBars.forEachIndexed { idx, bar ->
                        val ratio = (bar.total / maxBarVal).toFloat().coerceIn(0.05f, 1f)
                        val barH = if (bar.total <= 0.0) 6.dp.toPx() else (maxBarH * ratio)
                        val left = idx * slotW + (slotW - barW) / 2f
                        val top = maxBarH - barH

                        drawRoundRect(
                            color = if (bar.total > 0.0) theme.accent else Color.White.copy(alpha = 0.14f),
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(barW, barH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    monthlyBars.forEach { bar ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = bar.monthLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textSecondary
                            )
                            Text(
                                text = if (bar.total > 0) String.format(Locale.US, "$%.0f", bar.total) else "$0",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (bar.total > 0) theme.textPrimary else theme.textSecondary
                            )
                        }
                    }
                }
            }

            // Document picker strip
            if (candidateDocs.isNotEmpty()) {
                Text(
                    text = "SELECT DOCUMENT TO PARSE",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(candidateDocs) { docItem ->
                        val isSelected = selectedDoc?.id == docItem.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) theme.accent else theme.surface)
                                .pressScaleClickable(
                                    onClick = { selectedDoc = docItem }
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = docItem.name,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) BentoTextBlack else theme.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Parsed fields: Merchant, Date, Total
            OutlinedTextField(
                value = merchantDraft,
                onValueChange = { merchantDraft = it },
                label = { Text("Merchant (OCR Parsed)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = dateDraft,
                    onValueChange = { dateDraft = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = totalDraft,
                    onValueChange = { totalDraft = it },
                    label = { Text("Total ($)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Expense Category Chips
            Text(
                text = "EXPENSE CATEGORY",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DocumentSmartTools.expenseCategories) { cat ->
                    val isSelected = categoryDraft.equals(cat, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.bentoLime else theme.surface)
                            .pressScaleClickable(
                                testTag = "receipt_category_chip_${cat.lowercase().replace(" ", "_")}",
                                onClick = { categoryDraft = cat }
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
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
                    text = "Export CSV",
                    icon = Icons.Default.FileDownload,
                    onClick = {
                        scanViewModel.exportReceiptsCsv(context) { chooser ->
                            context.startActivity(chooser)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "receipt_export_csv_button"
                )
                GlassButton(
                    text = "Save Receipt",
                    accentFill = theme.accent,
                    onClick = {
                        val d = selectedDoc
                        if (d != null) {
                            val amount = totalDraft.replace(",", ".").toDoubleOrNull() ?: 0.0
                            scanViewModel.saveReceiptExpenseMetadata(
                                doc = d,
                                merchant = merchantDraft,
                                dateIso = dateDraft,
                                totalAmount = amount,
                                expenseCategory = categoryDraft
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "receipt_save_button"
                )
            }
        }
    }
}

@Composable
private fun BusinessCardCenterGlassSheet(
    documents: List<DocumentRecord>,
    initialDoc: DocumentRecord? = null,
    scanViewModel: ScanViewModel,
    hazeState: HazeState?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val candidateDocs = remember(documents) {
        documents.sortedByDescending { it.timestamp }
    }
    var selectedDoc by remember(candidateDocs, initialDoc) {
        mutableStateOf(
            initialDoc ?: candidateDocs.firstOrNull {
                it.category.equals("ID", ignoreCase = true) || it.ocrText.contains("@")
            } ?: candidateDocs.firstOrNull()
        )
    }

    val parsedCard: BusinessCardParsedData = remember(selectedDoc) {
        val d = selectedDoc
        if (d != null) {
            DocumentSmartTools.parseBusinessCardOcr(d.ocrText, d.name)
        } else {
            BusinessCardParsedData(
                fullName = "Alex Morgan",
                phone = "+1 (555) 234-5678",
                email = "alex@dynestudio.io",
                company = "Dyne Studio",
                jobTitle = "Product Lead"
            )
        }
    }

    var nameDraft by remember(parsedCard) { mutableStateOf(parsedCard.fullName) }
    var phoneDraft by remember(parsedCard) { mutableStateOf(parsedCard.phone) }
    var emailDraft by remember(parsedCard) { mutableStateOf(parsedCard.email) }
    var companyDraft by remember(parsedCard) { mutableStateOf(parsedCard.company) }
    var jobTitleDraft by remember(parsedCard) { mutableStateOf(parsedCard.jobTitle) }

    GlassSheet(onDismissRequest = onDismiss, hazeState = hazeState) {
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
                        text = "Business Card Scanner",
                        style = type.title,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "OCR extracts Name, Phone, Email & Company → Save to Contacts",
                        style = type.caption,
                        color = theme.bentoSky
                    )
                }
                ProBadge()
            }

            if (candidateDocs.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(candidateDocs) { docItem ->
                        val isSelected = selectedDoc?.id == docItem.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) theme.bentoSky else theme.surface)
                                .pressScaleClickable(onClick = { selectedDoc = docItem })
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = docItem.name,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) BentoTextBlack else theme.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it },
                label = { Text("Full Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.bentoSky,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phoneDraft,
                onValueChange = { phoneDraft = it },
                label = { Text("Phone Number") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.bentoSky,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = emailDraft,
                onValueChange = { emailDraft = it },
                label = { Text("Email Address") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.bentoSky,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = companyDraft,
                    onValueChange = { companyDraft = it },
                    label = { Text("Company") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.bentoSky,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = jobTitleDraft,
                    onValueChange = { jobTitleDraft = it },
                    label = { Text("Role / Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.bentoSky,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            GlassButton(
                text = "Save to contacts",
                icon = Icons.Default.ContactPage,
                accentFill = theme.bentoSky,
                onClick = {
                    val intent = DocumentSmartTools.createInsertContactIntent(
                        BusinessCardParsedData(
                            fullName = nameDraft,
                            phone = phoneDraft,
                            email = emailDraft,
                            company = companyDraft,
                            jobTitle = jobTitleDraft
                        )
                    )
                    runCatching {
                        context.startActivity(intent)
                        scanViewModel.showToast("Opened system Contacts for $nameDraft")
                    }.onFailure {
                        scanViewModel.showToast("No Contacts app found on device")
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "business_card_save_contacts_button"
            )
        }
    }
}

@Composable
private fun DocumentOrganizeExpirySheet(
    doc: DocumentRecord,
    isPro: Boolean,
    hazeState: HazeState?,
    onRequirePackFeature: (DocumentPackFeature, () -> Unit) -> Unit,
    onSave: (String, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var folderDraft by remember(doc.id) { mutableStateOf(doc.folderName) }
    var tagsDraft by remember(doc.id) { mutableStateOf(doc.tagsCsv) }
    var selectedExpiryDays by remember(doc.id) {
        val currentMs = doc.expiryEpochMs
        val daysLeft = if (currentMs > System.currentTimeMillis()) {
            ((currentMs - System.currentTimeMillis()) / (24L * 3600_000L)).toInt()
        } else {
            0
        }
        mutableStateOf(daysLeft)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val expiryMs = if (selectedExpiryDays > 0) {
            System.currentTimeMillis() + selectedExpiryDays * 24L * 3600_000L
        } else {
            0L
        }
        onSave(folderDraft, tagsDraft, expiryMs)
    }

    GlassSheet(onDismissRequest = onDismiss, hazeState = hazeState) {
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
                Text(
                    text = "Folders, Tags & Expiry Reminders",
                    style = type.title,
                    color = theme.textPrimary
                )
                if (!isPro) ProBadge()
            }

            OutlinedTextField(
                value = folderDraft,
                onValueChange = { folderDraft = it },
                label = { Text("Folder Name (e.g. Taxes, Travel, Medical)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tagsDraft,
                onValueChange = { tagsDraft = it },
                label = { Text("Tags (comma-separated, e.g. urgent, 2026, deductible)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "ID OR BILL EXPIRY REMINDER (30d, 7d, 1d before)",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.bentoCoral
            )
            Text(
                text = "Schedules local notifications 30 days, 7 days, and 1 day before expiry.",
                style = type.caption,
                color = theme.textSecondary
            )

            val dayPresets = listOf(0 to "None", 7 to "In 7 Days", 30 to "In 30 Days", 60 to "In 60 Days", 365 to "In 1 Year")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dayPresets) { (days, label) ->
                    val selected = selectedExpiryDays == days
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.bentoCoral else theme.surface)
                            .pressScaleClickable(
                                testTag = "expiry_preset_$days",
                                onClick = { selectedExpiryDays = days }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) BentoTextBlack else theme.textPrimary
                        )
                    }
                }
            }

            GlassButton(
                text = "Save Organization & Reminder",
                icon = Icons.Default.NotificationsActive,
                accentFill = theme.accent,
                onClick = {
                    val targetFeature = if (selectedExpiryDays > 0) {
                        DocumentPackFeature.SMART_EXPIRY_REMINDERS
                    } else {
                        DocumentPackFeature.SMART_FOLDERS_TAGS
                    }
                    onRequirePackFeature(targetFeature) {
                        if (selectedExpiryDays > 0 &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val expiryMs = if (selectedExpiryDays > 0) {
                                System.currentTimeMillis() + selectedExpiryDays * 24L * 3600_000L
                            } else {
                                0L
                            }
                            onSave(folderDraft, tagsDraft, expiryMs)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_organize_expiry_button"
            )
        }
    }
}

@Composable
private fun DocumentPasswordProtectSheet(
    doc: DocumentRecord,
    hazeState: HazeState?,
    onProtect: (String) -> Unit,
    onUnlock: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var passwordDraft by remember { mutableStateOf("") }

    GlassSheet(onDismissRequest = onDismiss, hazeState = hazeState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PDF Password Protect & Unlock",
                    style = type.title,
                    color = theme.textPrimary
                )
                ProBadge()
            }
            Text(
                text = "Powered by Apache-2.0 PdfBox-Android standard 128-bit encryption.",
                style = type.caption,
                color = theme.textSecondary
            )

            OutlinedTextField(
                value = passwordDraft,
                onValueChange = { passwordDraft = it },
                label = { Text("PDF Password") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
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
                    text = "Encrypt PDF",
                    icon = Icons.Default.Lock,
                    accentFill = theme.accent,
                    onClick = { if (passwordDraft.isNotBlank()) onProtect(passwordDraft) },
                    modifier = Modifier.weight(1f),
                    testTag = "doc_encrypt_pdf_button"
                )
                if (doc.isPasswordProtected) {
                    GlassButton(
                        text = "Remove Password",
                        icon = Icons.Default.LockOpen,
                        onClick = { onUnlock(passwordDraft) },
                        modifier = Modifier.weight(1f),
                        testTag = "doc_decrypt_pdf_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDocumentGlassSheet(
    currentName: String,
    hazeState: HazeState?,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var draftName by remember { mutableStateOf(currentName) }

    GlassSheet(onDismissRequest = onDismiss, hazeState = hazeState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Rename Document", style = type.title, color = theme.textPrimary)
            OutlinedTextField(
                value = draftName,
                onValueChange = { draftName = it },
                label = { Text("Document Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GlassButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                GlassButton(
                    text = "Save Name",
                    accentFill = theme.accent,
                    onClick = { if (draftName.isNotBlank()) onRename(draftName.trim()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MergeDocumentGlassSheet(
    targetDoc: DocumentRecord,
    candidateDocs: List<DocumentRecord>,
    hazeState: HazeState?,
    onSelectMergeSource: (DocumentRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    GlassSheet(onDismissRequest = onDismiss, hazeState = hazeState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Merge into \"${targetDoc.name}\"",
                style = type.title,
                color = theme.textPrimary
            )
            if (candidateDocs.isEmpty()) {
                Text(
                    text = "Scan at least one more document to merge its pages into this document.",
                    style = type.body,
                    color = theme.textSecondary
                )
            } else {
                Text(
                    text = "Select a document below to append its pages and rebuild the combined PDF:",
                    style = type.caption,
                    color = theme.textSecondary
                )
                candidateDocs.forEach { candidate ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(theme.surface)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                            .pressScaleClickable(onClick = { onSelectMergeSource(candidate) })
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = candidate.name, style = type.section, color = theme.textPrimary)
                            Text(
                                text = "${candidate.pageCount} pages • ${candidate.category}",
                                style = type.caption,
                                color = theme.accent
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.MergeType,
                            contentDescription = "Merge",
                            tint = theme.accent
                        )
                    }
                }
            }
            GlassButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Scans Tab:
 * - Search (above)
 * - Chips (All, QR, Barcode, Favorites) + CSV export in a three-dot menu
 * - A "0 saved scans" (or "N saved scans") caption on its own line
 * - A full-width "Scan QR / Barcode" button below the chips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScansVaultSection(
    scans: List<ScanRecord>,
    isPro: Boolean,
    filterCategory: ScanFilterCategory,
    onSelectFilter: (ScanFilterCategory) -> Unit,
    onScanNewCode: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenPaywall: () -> Unit,
    onInspectScan: (ScanRecord) -> Unit,
    onToggleFavorite: (ScanRecord) -> Unit,
    onDelete: (ScanRecord) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val dateFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val visibleScans = remember(scans, isPro) {
        if (isPro) scans else scans.take(10)
    }

    // Chips row (All, QR, Barcode, Favorites) + three-dot menu for CSV export
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier
                .weight(1f)
                .horizontalChipRowEdgeFade()
        ) {
            items(ScanFilterCategory.entries) { category ->
                val selected = filterCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) theme.accent else theme.surface)
                        .border(
                            1.dp,
                            if (selected) theme.accent else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(14.dp)
                        )
                        .pressScaleClickable(
                            testTag = "scan_filter_chip_${category.name.lowercase()}",
                            onClick = { onSelectFilter(category) }
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category.label,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) BentoTextBlack else theme.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Three-dot menu containing CSV export
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .pressScaleClickable(
                        testTag = "scans_three_dot_menu_button",
                        onClick = { showOverflowMenu = true }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Scans Menu",
                    tint = theme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isPro) "Export CSV" else "Export CSV (10 coins / Pro)",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = if (isPro) theme.accent else ProGold
                        )
                    },
                    onClick = {
                        showOverflowMenu = false
                        onExportCsv()
                    },
                    modifier = Modifier.testTag("library_export_csv_button")
                )
            }
        }
    }

    // Full-width "Scan QR / Barcode" button below the chips
    GlassButton(
        text = "Scan QR / Barcode",
        icon = Icons.Default.QrCode2,
        accentFill = theme.bentoSky,
        onClick = onScanNewCode,
        modifier = Modifier.fillMaxWidth(),
        testTag = "library_scan_qr_button"
    )

    // "0 saved scans" (or "N saved scans") caption on its own line
    Text(
        text = "${scans.size} ${if (scans.size == 1) "saved scan" else "saved scans"}",
        style = type.caption.copy(fontWeight = FontWeight.Bold),
        color = theme.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scans_saved_count_caption")
    )

    if (scans.isEmpty()) {
        VaultEmptyCard(
            icon = Icons.Default.QrCode2,
            accentColor = theme.bentoSky,
            title = "No QR or Barcodes Yet",
            description = "Scan any QR, Data Matrix, Aztec, PDF417, EAN, UPC, Code 128, Code 39, Code 93, ITF, or Codabar barcode with CameraX or from a gallery photo.",
            buttonText = "Scan QR / Barcode",
            buttonTestTag = "library_empty_scan_qr_button",
            onAction = onScanNewCode
        )
    } else {
        var scanBannerSlots = 0
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visibleScans.forEachIndexed { index, scan ->
                key(scan.id) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart ||
                                value == SwipeToDismissBoxValue.StartToEnd
                            ) {
                                onDelete(scan)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(theme.bentoCoral.copy(alpha = 0.25f))
                                    .padding(horizontal = 20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Scan",
                                    tint = theme.bentoCoral
                                )
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(theme.surface)
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                                .pressScaleClickable(onClick = { onInspectScan(scan) })
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(theme.bentoSky.copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${scan.formatName} • ${scan.parsedContentType.displayLabel}",
                                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                                            color = theme.bentoSky,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = dateFmt.format(Date(scan.timestamp)),
                                        style = type.caption,
                                        color = theme.textSecondary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = scan.displayTitle,
                                    style = type.body.copy(fontWeight = FontWeight.Bold),
                                    color = theme.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = scan.rawContent,
                                    style = type.caption,
                                    color = theme.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = if (scan.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (scan.isFavorite) theme.accent else theme.textSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .pressScaleClickable(onClick = { onToggleFavorite(scan) })
                                )
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = theme.bentoCoral,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .pressScaleClickable(onClick = { onDelete(scan) })
                                )
                            }
                        }
                    }
                }

                // Banner after every 6th item (reserving 1 slot for the end so max 2 ad slots per screen)
                if ((index + 1) % 6 == 0 && index < visibleScans.lastIndex && scanBannerSlots < 1) {
                    scanBannerSlots++
                    InlineBannerAdSlot(placementTag = "scans_after_${index + 1}")
                }
            }

            if (!isPro && scans.size > 10) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(theme.surface)
                        .proBorder(RoundedCornerShape(22.dp))
                        .pressScaleClickable(
                            testTag = "scans_unlock_history_card",
                            onClick = onOpenPaywall
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${scans.size - 10} Older Scans Locked",
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                            ProBadge()
                        }
                        Text(
                            text = "Free keeps the last 10 scans visible. Unlock Dyne Pro for unlimited scan history & CSV export.",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Older Scans",
                        tint = ProGold
                    )
                }
            }
        }
    }

    // Reserved banner slot at the end of the Scans list (max 2 ad slots per screen)
    InlineBannerAdSlot(placementTag = "scans_list_end")
}

@Composable
private fun VaultEmptyCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String,
    buttonText: String,
    buttonTestTag: String? = null,
    onAction: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(28.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.18f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(text = title, style = type.title, color = theme.textPrimary, textAlign = TextAlign.Center)
        Text(
            text = description,
            style = type.body,
            color = theme.textSecondary,
            textAlign = TextAlign.Center
        )
        GlassButton(
            text = buttonText,
            accentFill = accentColor,
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
            testTag = buttonTestTag
        )
    }
}

private fun authenticateWithBiometrics(
    context: Context,
    title: String,
    subtitle: String,
    onAuthenticated: () -> Unit,
    onInfoMessage: (String) -> Unit
) {
    val activity = context.findFragmentActivity()
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    val biometricManager = BiometricManager.from(context)
    val canAuth = biometricManager.canAuthenticate(authenticators)

    if (activity != null && canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onInfoMessage(errString.toString())
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(promptInfo)
    } else {
        onAuthenticated()
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
