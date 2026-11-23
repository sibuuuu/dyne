package com.example.ui.media

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.PointF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.LocalAdManager
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.billing.QuotaAllowanceChip
import com.example.billing.QuotaType
import com.example.data.DeviceCapabilities
import com.example.data.MediaRecentFileRecord
import com.example.data.PdfPageSize
import com.example.data.PreferencesRepository
import com.example.data.ScanRepository
import com.example.data.UserPreferences
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.GlassSurface
import com.example.ui.scan.ScanViewModel
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.ProGold
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val DyneGold = ProGold

private data class MediaGlassTokens(
    val textPrimary: Color,
    val textSecondary: Color,
    val cardBackground: Color,
    val cardBorder: Color
)

private object LocalGlassTokens {
    val current: MediaGlassTokens
        @Composable
        get() {
            val t = LocalDyneTheme.current
            return MediaGlassTokens(
                textPrimary = t.textPrimary,
                textSecondary = t.textSecondary,
                cardBackground = t.surface,
                cardBorder = Color.White.copy(alpha = 0.12f)
            )
        }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassSurface(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    DyneSegmentedControl(
        segments = options,
        selectedIndex = selectedIndex,
        onSegmentSelected = onSelected
    )
}

@Composable
private fun ProBadge(text: String = "PRO") {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ProGold.copy(alpha = 0.22f))
            .border(1.dp, ProGold.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = ProGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Shared first-use save location tip card + cancelable background progress bar +
 * before/after Result Screen + shared Room "Recent files" list.
 */
@Composable
fun MediaSaveLocationTipBanner(
    prefs: UserPreferences,
    preferencesRepository: PreferencesRepository?
) {
    val scope = rememberCoroutineScope()
    val tokens = LocalGlassTokens.current
    AnimatedVisibility(visible = !prefs.mediaSaveLocationTipDismissed) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("media_save_location_tip_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Save Location Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "100% On-Device • Saved via MediaStore",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "Photos save to Pictures/Dyne, audio to Music/Dyne (or Ringtones), videos to Movies/Dyne, and PDFs to Downloads/Dyne. Zero storage permissions required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary
                    )
                }
                IconButton(
                    onClick = { scope.launch { preferencesRepository?.dismissMediaSaveLocationTip() } },
                    modifier = Modifier.testTag("dismiss_media_save_tip_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss tip",
                        tint = tokens.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MediaProcessingProgressCard(
    isProcessing: Boolean,
    progressCurrent: Int,
    progressTotal: Int,
    statusText: String,
    onCancel: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    AnimatedVisibility(visible = isProcessing) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("media_processing_progress_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText.ifBlank { "Processing on device..." },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("media_processing_cancel_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
                val fraction = if (progressTotal > 0) {
                    (progressCurrent.toFloat() / progressTotal.toFloat()).coerceIn(0.05f, 1f)
                } else 0.5f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun MediaExportResultCard(
    result: MediaExportResult?,
    onConvertAnother: () -> Unit,
    onToast: (String) -> Unit
) {
    if (result == null) return
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    var currentSavedPath by remember(result) { mutableStateOf(result.savedMediaStorePath) }
    var currentSavedUri by remember(result) { mutableStateOf(result.savedMediaStoreUri) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            .testTag("media_export_result_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Export Complete",
                    tint = Color(0xFF28C840),
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = result.summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary
                    )
                }
            }

            // Before / After visual preview when bitmaps are available
            if (result.beforePreviewBitmap != null || result.afterPreviewBitmap != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    result.beforePreviewBitmap?.let { bmp ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("BEFORE", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                            Spacer(Modifier.height(4.dp))
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Before preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                    result.afterPreviewBitmap?.let { bmp ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("AFTER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "After preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }

            // Before / After File Size Comparison Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.cardBackground.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Before Size", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                    Text(
                        text = ScanRepository.formatFileSize(result.beforeSizeBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary
                    )
                }
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = tokens.textSecondary)
                Column(horizontalAlignment = Alignment.End) {
                    Text("After Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = ScanRepository.formatFileSize(result.afterSizeBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!currentSavedPath.isNullOrBlank()) {
                Text(
                    text = "Saved to: $currentSavedPath",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF28C840)
                )
            }

            // Action Buttons: Share, Save, Open, Convert another
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = MediaStudioEngine.createShareIntentForFile(context, result.localCacheFile, result.mimeType)
                        if (intent != null) {
                            context.startActivity(Intent.createChooser(intent, "Share ${result.fileName}"))
                        } else {
                            onToast("Unable to share file.")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("media_result_share_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }

                OutlinedButton(
                    onClick = {
                        val cat = when {
                            result.mimeType.startsWith("image/") -> MediaStudioEngine.MediaStoreCategory.IMAGE
                            result.mimeType.startsWith("audio/") -> MediaStudioEngine.MediaStoreCategory.AUDIO
                            result.mimeType.startsWith("video/") -> MediaStudioEngine.MediaStoreCategory.VIDEO
                            else -> MediaStudioEngine.MediaStoreCategory.DOWNLOAD
                        }
                        val (path, uri) = MediaStudioEngine.saveFileToMediaStore(context, result.localCacheFile, result.mimeType, cat)
                        if (path != null) {
                            currentSavedPath = path
                            currentSavedUri = uri
                            onToast("Saved to $path")
                        } else {
                            onToast("Saved copy to app media output")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("media_result_save_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }

                OutlinedButton(
                    onClick = {
                        val openIntent = MediaStudioEngine.createOpenIntentForFile(
                            context,
                            result.localCacheFile,
                            currentSavedUri,
                            result.mimeType
                        )
                        if (openIntent != null) {
                            runCatching { context.startActivity(openIntent) }
                                .onFailure { onToast("No external viewer app installed for ${result.mimeType}") }
                        } else {
                            onToast("File ready at ${currentSavedPath ?: result.localCacheFile.name}")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("media_result_open_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open")
                }
            }

            OutlinedButton(
                onClick = onConvertAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("media_result_convert_another_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Convert another")
            }
        }
    }
}

@Composable
fun MediaStudioRecentFilesSection(
    scanViewModel: ScanViewModel?,
    onToast: (String) -> Unit
) {
    if (scanViewModel == null) return
    val recentFiles by scanViewModel.allMediaRecentFiles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("media_recent_files_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Media Studio Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "Shared Room history across Photo, Audio, Video & PDF tools",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary
                    )
                }
                if (recentFiles.isNotEmpty()) {
                    IconButton(
                        onClick = { scanViewModel.clearAllMediaRecentFiles() },
                        modifier = Modifier.testTag("clear_recent_media_files_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear recent files",
                            tint = tokens.textSecondary
                        )
                    }
                }
            }

            if (recentFiles.isEmpty()) {
                Text(
                    text = "Processed photos, cutouts, audio tracks, videos, and PDFs will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary
                )
            } else {
                recentFiles.take(8).forEach { rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.cardBackground.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${rec.toolName} • ${ScanRepository.formatFileSize(rec.afterSizeBytes)} • ${dateFormat.format(Date(rec.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                val file = File(rec.localCachePath)
                                val intent = MediaStudioEngine.createShareIntentForFile(context, file, rec.mimeType)
                                if (intent != null) {
                                    context.startActivity(Intent.createChooser(intent, "Share ${rec.fileName}"))
                                } else {
                                    onToast("Cached file was cleaned; saved copy remains in ${rec.outputUriString.ifBlank { "device MediaStore" }}")
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share recent file", tint = tokens.textPrimary)
                        }
                        IconButton(
                            onClick = { scanViewModel.deleteMediaRecentFile(rec.id) }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove from list", tint = tokens.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

private fun recordAndNotifyFinishedExport(
    context: Context,
    scanViewModel: ScanViewModel?,
    adManager: com.example.ads.AdManager?,
    result: MediaExportResult
) {
    scanViewModel?.recordMediaRecentFile(
        fileName = result.fileName,
        toolId = result.toolId,
        toolName = result.toolName,
        mimeType = result.mimeType,
        outputUriString = result.savedMediaStorePath ?: "",
        localCachePath = result.localCacheFile.absolutePath,
        beforeSizeBytes = result.beforeSizeBytes,
        afterSizeBytes = result.afterSizeBytes,
        summaryLabel = result.summaryText
    )
    // Ads rule: never on editor canvases or during processing; at most one interstitial after a finished export.
    val activity = context as? Activity
    if (activity != null) {
        adManager?.maybeShowInterstitialAtNaturalBreak(
            activity = activity,
            reason = "After Media Studio export (${result.toolName})"
        )
    }
}

// ============================================================================
// 1. PHOTO CONVERTER (Free: 1 image ≤12 MP; Pro: batch up to 50, full res)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoConverterToolCard(
    activeModeId: String,
    isPro: Boolean,
    prefs: UserPreferences,
    preferencesRepository: PreferencesRepository?,
    scanViewModel: ScanViewModel?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGlassTokens.current
    val featureGate = LocalFeatureGate.current
    val adManager = LocalAdManager.current
    val isBatchProUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.PHOTO_CONVERTER_PRO) == true)

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var previewBitmap by remember { mutableStateOf(MediaStudioEngine.createSampleStudioBitmap()) }
    var outputFormat by remember { mutableStateOf(ImageOutputFormat.JPG) }
    var quality by remember { mutableFloatStateOf(88f) }
    var resizeMode by remember { mutableStateOf(PhotoResizeMode.ORIGINAL) }
    var resizePercent by remember { mutableFloatStateOf(100f) }
    var exactWidthText by remember { mutableStateOf("1920") }
    var exactHeightText by remember { mutableStateOf("1080") }
    var keepExif by remember { mutableStateOf(false) }
    var renamePattern by remember { mutableStateOf("{name}_{date}") }

    var activeJob by remember { mutableStateOf<Job?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressCurrent by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(1) }
    var progressStatus by remember { mutableStateOf("") }
    var exportResult by remember { mutableStateOf<MediaExportResult?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUris = listOf(uri)
            exportResult = null
            scope.launch {
                MediaStudioEngine.decodeBitmapFromUri(context, uri, maxMegapixels = if (isBatchProUnlocked) 0 else 12)?.let {
                    previewBitmap = it
                }
            }
        }
    }

    val batchPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris.take(50)
            exportResult = null
            scope.launch {
                MediaStudioEngine.decodeBitmapFromUri(context, uris.first(), maxMegapixels = 0)?.let {
                    previewBitmap = it
                }
            }
        }
    }

    val safFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUris = listOf(uri)
            exportResult = null
            scope.launch {
                MediaStudioEngine.decodeBitmapFromUri(context, uri, maxMegapixels = if (isBatchProUnlocked) 0 else 12)?.let {
                    previewBitmap = it
                }
            }
        }
    }

    val estimatedBytes = remember(previewBitmap, outputFormat, quality, resizeMode, resizePercent, exactWidthText, exactHeightText, selectedUris.size) {
        val resizedSample = MediaStudioEngine.applyResizeToBitmap(
            previewBitmap,
            resizeMode,
            resizePercent.roundToInt(),
            exactWidthText.toIntOrNull() ?: 1920,
            exactHeightText.toIntOrNull() ?: 1080
        )
        val singleEst = MediaStudioEngine.estimateCompressedImageBytes(resizedSample, outputFormat, quality.roundToInt())
        singleEst * maxOf(1, selectedUris.size)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("photo_converter_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MediaSaveLocationTipBanner(prefs = prefs, preferencesRepository = preferencesRepository)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Photo Converter & Resizer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Input: JPG, PNG, WebP, HEIC/HEIF, BMP, GIF (1st frame)" +
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) ", AVIF" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                    }
                    if (!isBatchProUnlocked) {
                        QuotaAllowanceChip(
                            quotaType = QuotaType.PHOTO_CONVERTER,
                            isProUnlocked = false,
                            prefs = prefs,
                            onClick = { featureGate?.openQuotaUnlockSheet(QuotaType.PHOTO_CONVERTER) }
                        )
                    } else {
                        ProBadge(text = "PRO BATCH 50")
                    }
                }

                // Picker buttons (System Photo Picker + SAF)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            singlePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("photo_converter_pick_single_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Photo")
                    }

                    OutlinedButton(
                        onClick = {
                            if (isBatchProUnlocked) {
                                batchPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                featureGate?.require(
                                    feature = PremiumFeature.PHOTO_CONVERTER_PRO,
                                    onUnlocked = {
                                        batchPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("photo_converter_pick_batch_btn")
                    ) {
                        Text("Batch (Up to 50)")
                        if (!isBatchProUnlocked) {
                            Spacer(Modifier.width(4.dp))
                            ProBadge(text = "PRO")
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        safFilePicker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_converter_saf_btn")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Browse Files (HEIC / AVIF / BMP / GIF / WebP)")
                }

                Text(
                    text = if (selectedUris.isEmpty()) {
                        "Previewing studio sample (${previewBitmap.width}×${previewBitmap.height} px) • Tap Pick Photo or Convert below"
                    } else {
                        "Selected ${selectedUris.size} image(s) • Primary: ${MediaStudioEngine.resolveDisplayName(context, selectedUris.first())}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Output Format Segmented Control
                Text("Output Format", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                GlassSegmentedControl(
                    options = ImageOutputFormat.entries.map { it.label },
                    selectedIndex = outputFormat.ordinal,
                    onSelected = { idx -> outputFormat = ImageOutputFormat.entries[idx] }
                )

                // Quality Slider + Live Size Estimate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quality: ${quality.roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "Live Est: ~${ScanRepository.formatFileSize(estimatedBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("photo_converter_live_size_estimate")
                    )
                }
                Slider(
                    value = quality,
                    onValueChange = { quality = it },
                    valueRange = 15f..100f,
                    modifier = Modifier.testTag("photo_converter_quality_slider")
                )

                // Resize Mode Chips
                Text("Resize Mode", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PhotoResizeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = resizeMode == mode,
                            onClick = { resizeMode = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }

                if (resizeMode == PhotoResizeMode.PERCENT) {
                    Text(
                        text = "Scale: ${resizePercent.roundToInt()}% (${(previewBitmap.width * resizePercent / 100f).roundToInt()}×${(previewBitmap.height * resizePercent / 100f).roundToInt()} px)",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textPrimary
                    )
                    Slider(
                        value = resizePercent,
                        onValueChange = { resizePercent = it },
                        valueRange = 20f..150f
                    )
                } else if (resizeMode == PhotoResizeMode.EXACT) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = exactWidthText,
                            onValueChange = { exactWidthText = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Width (px)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = exactHeightText,
                            onValueChange = { exactHeightText = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Height (px)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // Keep or Strip EXIF Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (keepExif) "Keep EXIF Metadata" else "Strip EXIF & GPS Metadata",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Stripping EXIF removes camera & GPS location tags for privacy",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                    }
                    Switch(
                        checked = keepExif,
                        onCheckedChange = { keepExif = it },
                        modifier = Modifier.testTag("photo_converter_exif_switch")
                    )
                }

                // Rename Pattern
                OutlinedTextField(
                    value = renamePattern,
                    onValueChange = { renamePattern = it },
                    label = { Text("Rename Pattern ({name}, {date}, {index})") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_converter_rename_input"),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val runConversion: () -> Unit = {
                            activeJob?.cancel()
                            exportResult = null
                            isProcessing = true
                            activeJob = scope.launch {
                                try {
                                    val res = MediaStudioEngine.convertPhotosBatch(
                                        context = context,
                                        uris = selectedUris,
                                        sampleFallbackBitmap = previewBitmap,
                                        outputFormat = outputFormat,
                                        quality = quality.roundToInt(),
                                        resizeMode = resizeMode,
                                        resizePercent = resizePercent.roundToInt(),
                                        exactWidth = exactWidthText.toIntOrNull() ?: 1920,
                                        exactHeight = exactHeightText.toIntOrNull() ?: 1080,
                                        keepExif = keepExif,
                                        renamePattern = renamePattern,
                                        isProUnlocked = isBatchProUnlocked,
                                        onProgress = { cur, tot, msg ->
                                            progressCurrent = cur
                                            progressTotal = tot
                                            progressStatus = msg
                                        }
                                    )
                                    exportResult = res
                                    recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                                } catch (_: CancellationException) {
                                    statusBanner = "Conversion cancelled."
                                } catch (e: Exception) {
                                    statusBanner = "Error: ${e.message ?: "Unsupported image format."}"
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                        if (isBatchProUnlocked || featureGate == null) {
                            runConversion()
                        } else {
                            featureGate.requireQuotaOrUnlock(
                                quotaType = QuotaType.PHOTO_CONVERTER,
                                onAllowed = runConversion
                            )
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_converter_run_btn")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedUris.size > 1) "Convert ${selectedUris.size} Photos to ${outputFormat.label}"
                        else "Convert Photo to ${outputFormat.label}"
                    )
                }

                statusBanner?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        MediaProcessingProgressCard(
            isProcessing = isProcessing,
            progressCurrent = progressCurrent,
            progressTotal = progressTotal,
            statusText = progressStatus,
            onCancel = {
                activeJob?.cancel()
                isProcessing = false
                statusBanner = "Conversion cancelled."
            }
        )

        MediaExportResultCard(
            result = exportResult,
            onConvertAnother = {
                exportResult = null
                selectedUris = emptyList()
            },
            onToast = { statusBanner = it }
        )

        MediaStudioRecentFilesSection(
            scanViewModel = scanViewModel,
            onToast = { statusBanner = it }
        )
    }
}

// ============================================================================
// 2. BACKGROUND REMOVER (Free: 3/day, then rewarded ad or Premium; HD export Pro)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackgroundRemoverToolCard(
    activeModeId: String,
    isPro: Boolean,
    prefs: UserPreferences,
    preferencesRepository: PreferencesRepository?,
    scanViewModel: ScanViewModel?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGlassTokens.current
    val featureGate = LocalFeatureGate.current
    val adManager = LocalAdManager.current
    val isProUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.BACKGROUND_REMOVER_PRO) == true)

    val todayStr = remember { java.time.LocalDate.now().toString() }
    val usedToday = if (prefs.bgRemoverDailyDate == todayStr) prefs.bgRemoverDailyCount else 0
    val remainingFreeToday = (3 - usedToday).coerceAtLeast(0) + prefs.bgRemoverBonusCredits

    var sourceBitmap by remember { mutableStateOf(MediaStudioEngine.createSampleStudioBitmap(portraitSubject = true)) }
    var maskAlpha by remember { mutableStateOf<IntArray?>(null) }
    var segmentationNote by remember {
        mutableStateOf(
            if (DeviceCapabilities.hasSubjectSegmentationClass()) {
                "On-device subject cutout ready"
            } else {
                "Portrait cutout mode active on this device"
            }
        )
    }
    var featherRadius by remember { mutableFloatStateOf(3f) }
    var backdropMode by remember { mutableStateOf("TRANSPARENT") } // TRANSPARENT, COLOR, GRADIENT, BLUR_ORIGINAL, PHOTO
    var solidBgColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var customBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var addDropShadow by remember { mutableStateOf(true) }
    var brushMode by remember { mutableStateOf("NONE") } // NONE, ERASE, RESTORE
    var brushSizeRatio by remember { mutableFloatStateOf(0.06f) }
    var exportFormat by remember { mutableStateOf(ImageOutputFormat.PNG) }
    var hdExportEnabled by remember { mutableStateOf(isProUnlocked) }

    var activeJob by remember { mutableStateOf<Job?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var exportResult by remember { mutableStateOf<MediaExportResult?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var maskVersionTick by remember { mutableIntStateOf(0) }

    val subjectPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            exportResult = null
            scope.launch {
                MediaStudioEngine.decodeBitmapFromUri(context, uri, maxMegapixels = if (isProUnlocked) 16 else 8)?.let { bmp ->
                    sourceBitmap = bmp
                    maskAlpha = null
                }
            }
        }
    }

    val backdropPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaStudioEngine.decodeBitmapFromUri(context, uri, maxMegapixels = 8)?.let { bmp ->
                    customBgBitmap = bmp
                    backdropMode = "PHOTO"
                }
            }
        }
    }

    val previewComposite = remember(
        sourceBitmap,
        maskAlpha,
        maskVersionTick,
        featherRadius,
        backdropMode,
        solidBgColor,
        customBgBitmap,
        addDropShadow
    ) {
        val alpha = maskAlpha
        if (alpha == null || alpha.size != sourceBitmap.width * sourceBitmap.height) {
            sourceBitmap
        } else {
            MediaStudioEngine.compositeSegmentedImage(
                source = sourceBitmap,
                baseMaskAlpha = alpha,
                featherRadiusPx = featherRadius.roundToInt(),
                backdropMode = backdropMode,
                solidBgColor = solidBgColor,
                gradientColors = AndroidColor.rgb(30, 60, 114) to AndroidColor.rgb(42, 82, 152),
                customBgBitmap = customBgBitmap,
                addDropShadow = addDropShadow,
                drawCheckerboardForPreview = true
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("background_remover_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MediaSaveLocationTipBanner(prefs = prefs, preferencesRepository = preferencesRepository)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Background Remover",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = segmentationNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                    }
                    if (isProUnlocked) {
                        ProBadge(text = "UNLIMITED HD")
                    } else {
                        QuotaAllowanceChip(
                            quotaType = QuotaType.BACKGROUND_REMOVER,
                            isProUnlocked = false,
                            prefs = prefs,
                            onClick = { featureGate?.openQuotaUnlockSheet(QuotaType.BACKGROUND_REMOVER) }
                        )
                    }
                }

                // Checkerboard Interactive Canvas Preview (supports Refine Brush)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, tokens.cardBorder, RoundedCornerShape(16.dp))
                        .pointerInput(brushMode, brushSizeRatio, maskAlpha) {
                            if (brushMode != "NONE" && maskAlpha != null) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val nx = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val ny = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    maskAlpha?.let { arr ->
                                        MediaStudioEngine.applyMaskBrushStroke(
                                            maskAlpha = arr,
                                            width = sourceBitmap.width,
                                            height = sourceBitmap.height,
                                            normPoints = listOf(PointF(nx, ny)),
                                            brushRadiusRatio = brushSizeRatio,
                                            restoreForeground = (brushMode == "RESTORE")
                                        )
                                        maskVersionTick++
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = previewComposite.asImageBitmap(),
                        contentDescription = "Segmented subject preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            subjectPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bg_remover_pick_photo_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Photo")
                    }

                    Button(
                        onClick = {
                            val runCutout: () -> Unit = {
                                activeJob?.cancel()
                                isProcessing = true
                                progressStatus = "Removing background..."
                                activeJob = scope.launch {
                                    try {
                                        val seg = MediaStudioEngine.runSubjectOrSelfieSegmentation(sourceBitmap)
                                        maskAlpha = seg.foregroundMaskAlpha
                                        segmentationNote = seg.statusNote
                                        maskVersionTick++
                                    } catch (_: CancellationException) {
                                        statusBanner = "Segmentation cancelled."
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                            if (isProUnlocked || featureGate == null) {
                                runCutout()
                            } else {
                                featureGate.requireQuotaOrUnlock(
                                    quotaType = QuotaType.BACKGROUND_REMOVER,
                                    onAllowed = runCutout
                                )
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bg_remover_run_cutout_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove BG")
                    }
                }

                // Refine Brush Controls (Off / Erase / Restore)
                Text("Refine Brush (Drag on Canvas)", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                GlassSegmentedControl(
                    options = listOf("Preview Only", "Erase Brush", "Restore Brush"),
                    selectedIndex = when (brushMode) {
                        "ERASE" -> 1
                        "RESTORE" -> 2
                        else -> 0
                    },
                    onSelected = { idx ->
                        brushMode = when (idx) {
                            1 -> "ERASE"
                            2 -> "RESTORE"
                            else -> "NONE"
                        }
                    }
                )

                // Edge Feather Slider
                Text(
                    text = "Edge Feather: ${featherRadius.roundToInt()} px",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textPrimary
                )
                Slider(
                    value = featherRadius,
                    onValueChange = { featherRadius = it },
                    valueRange = 0f..10f,
                    modifier = Modifier.testTag("bg_remover_feather_slider")
                )

                // New Background Options
                Text("New Background", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "TRANSPARENT" to "Transparent",
                        "COLOR" to "Solid Color",
                        "GRADIENT" to "Studio Gradient",
                        "BLUR_ORIGINAL" to "Blurred Original",
                        "PHOTO" to "Custom Photo"
                    ).forEach { (modeKey, label) ->
                        FilterChip(
                            selected = backdropMode == modeKey,
                            onClick = {
                                if (modeKey == "PHOTO" && customBgBitmap == null) {
                                    backdropPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    backdropMode = modeKey
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                if (backdropMode == "COLOR") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            AndroidColor.WHITE,
                            AndroidColor.rgb(214, 234, 248),
                            AndroidColor.rgb(25, 25, 35),
                            AndroidColor.rgb(255, 214, 102),
                            AndroidColor.rgb(42, 157, 143)
                        ).forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(col))
                                    .border(
                                        width = if (solidBgColor == col) 2.5.dp else 1.dp,
                                        color = if (solidBgColor == col) MaterialTheme.colorScheme.primary else tokens.cardBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { solidBgColor = col }
                            )
                        }
                    }
                }

                // Drop Shadow & HD Export Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subject Drop Shadow", style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
                    Switch(checked = addDropShadow, onCheckedChange = { addDropShadow = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("HD Full-Resolution Export", style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
                        if (!isProUnlocked) ProBadge(text = "PRO")
                    }
                    Switch(
                        checked = hdExportEnabled && isProUnlocked,
                        onCheckedChange = { wantHd ->
                            if (wantHd && !isProUnlocked) {
                                featureGate?.require(
                                    feature = PremiumFeature.BACKGROUND_REMOVER_PRO,
                                    onUnlocked = { hdExportEnabled = true }
                                )
                            } else {
                                hdExportEnabled = wantHd
                            }
                        }
                    )
                }

                // Export PNG or JPG buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            exportFormat = ImageOutputFormat.PNG
                            scope.launch {
                                val alpha = maskAlpha ?: MediaStudioEngine.runSubjectOrSelfieSegmentation(sourceBitmap).also {
                                    maskAlpha = it.foregroundMaskAlpha
                                    segmentationNote = it.statusNote
                                }.foregroundMaskAlpha

                                val finalBmp = MediaStudioEngine.compositeSegmentedImage(
                                    source = sourceBitmap,
                                    baseMaskAlpha = alpha,
                                    featherRadiusPx = featherRadius.roundToInt(),
                                    backdropMode = backdropMode,
                                    solidBgColor = solidBgColor,
                                    gradientColors = AndroidColor.rgb(30, 60, 114) to AndroidColor.rgb(42, 82, 152),
                                    customBgBitmap = customBgBitmap,
                                    addDropShadow = addDropShadow,
                                    drawCheckerboardForPreview = false // Real transparent alpha in PNG!
                                )
                                val outDir = File(context.cacheDir, "media_studio/bg_remover").apply { mkdirs() }
                                val outFile = File(outDir, "Dyne_Cutout_${System.currentTimeMillis()}.png")
                                FileOutputStream(outFile).use { finalBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                val (savedPath, savedUri) = MediaStudioEngine.saveFileToMediaStore(
                                    context, outFile, "image/png", MediaStudioEngine.MediaStoreCategory.IMAGE
                                )
                                val res = MediaExportResult(
                                    toolId = "background_remover",
                                    toolName = "Background Remover",
                                    fileName = outFile.name,
                                    mimeType = "image/png",
                                    localCacheFile = outFile,
                                    savedMediaStorePath = savedPath,
                                    savedMediaStoreUri = savedUri,
                                    beforeSizeBytes = MediaStudioEngine.estimateCompressedImageBytes(sourceBitmap, ImageOutputFormat.PNG, 100),
                                    afterSizeBytes = outFile.length(),
                                    beforePreviewBitmap = sourceBitmap,
                                    afterPreviewBitmap = previewComposite,
                                    summaryText = "Transparent PNG cutout (${finalBmp.width}×${finalBmp.height} px)"
                                )
                                exportResult = res
                                recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bg_remover_export_png_btn")
                    ) {
                        Text("Export Transparent PNG")
                    }

                    OutlinedButton(
                        onClick = {
                            exportFormat = ImageOutputFormat.JPG
                            scope.launch {
                                val alpha = maskAlpha ?: MediaStudioEngine.runSubjectOrSelfieSegmentation(sourceBitmap).also {
                                    maskAlpha = it.foregroundMaskAlpha
                                    segmentationNote = it.statusNote
                                }.foregroundMaskAlpha

                                val effectiveBackdrop = if (backdropMode == "TRANSPARENT") "COLOR" else backdropMode
                                val finalBmp = MediaStudioEngine.compositeSegmentedImage(
                                    source = sourceBitmap,
                                    baseMaskAlpha = alpha,
                                    featherRadiusPx = featherRadius.roundToInt(),
                                    backdropMode = effectiveBackdrop,
                                    solidBgColor = solidBgColor,
                                    gradientColors = AndroidColor.rgb(30, 60, 114) to AndroidColor.rgb(42, 82, 152),
                                    customBgBitmap = customBgBitmap,
                                    addDropShadow = addDropShadow,
                                    drawCheckerboardForPreview = false
                                )
                                val outDir = File(context.cacheDir, "media_studio/bg_remover").apply { mkdirs() }
                                val outFile = File(outDir, "Dyne_Cutout_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(outFile).use { finalBmp.compress(Bitmap.CompressFormat.JPEG, 94, it) }
                                val (savedPath, savedUri) = MediaStudioEngine.saveFileToMediaStore(
                                    context, outFile, "image/jpeg", MediaStudioEngine.MediaStoreCategory.IMAGE
                                )
                                val res = MediaExportResult(
                                    toolId = "background_remover",
                                    toolName = "Background Remover",
                                    fileName = outFile.name,
                                    mimeType = "image/jpeg",
                                    localCacheFile = outFile,
                                    savedMediaStorePath = savedPath,
                                    savedMediaStoreUri = savedUri,
                                    beforeSizeBytes = MediaStudioEngine.estimateCompressedImageBytes(sourceBitmap, ImageOutputFormat.JPG, 92),
                                    afterSizeBytes = outFile.length(),
                                    beforePreviewBitmap = sourceBitmap,
                                    afterPreviewBitmap = finalBmp,
                                    summaryText = "Studio JPG with backdrop (${finalBmp.width}×${finalBmp.height} px)"
                                )
                                exportResult = res
                                recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bg_remover_export_jpg_btn")
                    ) {
                        Text("Export JPG")
                    }
                }
            }
        }

        MediaProcessingProgressCard(
            isProcessing = isProcessing,
            progressCurrent = 1,
            progressTotal = 1,
            statusText = progressStatus,
            onCancel = {
                activeJob?.cancel()
                isProcessing = false
            }
        )

        MediaExportResultCard(
            result = exportResult,
            onConvertAnother = { exportResult = null },
            onToast = { statusBanner = it }
        )

        MediaStudioRecentFilesSection(
            scanViewModel = scanViewModel,
            onToast = { statusBanner = it }
        )
    }
}

// ============================================================================
// 3. PHOTO EDITOR (Crop, Adjust, 20 Filters, Draw, Text, Stickers, Frames, Collage, Meme)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoEditorToolCard(
    activeModeId: String,
    isPro: Boolean,
    prefs: UserPreferences,
    preferencesRepository: PreferencesRepository?,
    scanViewModel: ScanViewModel?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGlassTokens.current
    val featureGate = LocalFeatureGate.current
    val adManager = LocalAdManager.current
    val isEditorProUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.PHOTO_EDITOR_PRO) == true)

    var baseBitmap by remember { mutableStateOf(MediaStudioEngine.createSampleStudioBitmap()) }
    var collageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var editorTab by remember(activeModeId) {
        mutableStateOf(
            when (activeModeId) {
                "filters" -> "FILTERS"
                "draw_text" -> "DRAW_TEXT"
                "meme" -> "MEME_FRAME"
                "curves_grain" -> "PRO_TONE"
                "collage" -> "COLLAGE"
                else -> "CROP_ADJUST"
            }
        )
    }

    // Crop & Geometry
    var cropRatio by remember { mutableStateOf<Float?>(null) }
    var rotationDeg by remember { mutableIntStateOf(0) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var straightenDeg by remember { mutableFloatStateOf(0f) }

    // Adjust Sliders (9 Free + 3 Pro)
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var tint by remember { mutableFloatStateOf(0f) }
    var highlights by remember { mutableFloatStateOf(0f) }
    var shadows by remember { mutableFloatStateOf(0f) }
    var sharpen by remember { mutableFloatStateOf(0f) }
    var vignette by remember { mutableFloatStateOf(0f) }
    var curvesStrength by remember { mutableFloatStateOf(0f) }
    var grainAmount by remember { mutableFloatStateOf(0f) }
    var fadeAmount by remember { mutableFloatStateOf(0f) }

    // Filter (20 ColorMatrix filters: 6 Free, 14 Pro)
    var selectedFilter by remember { mutableStateOf(MediaStudioEngine.PHOTO_FILTERS.first()) }

    // Draw, Text, Stickers, Frames, Meme
    var drawTool by remember { mutableStateOf("PEN") } // PEN, HIGHLIGHTER, MOSAIC
    val mosaicPts = remember { mutableStateListOf<PointF>() }
    val penStrokes = remember { mutableStateListOf<Pair<List<PointF>, Int>>() }
    val highlighterStrokes = remember { mutableStateListOf<Pair<List<PointF>, Int>>() }
    val redoStack = remember { mutableStateListOf<String>() }
    var overlayText by remember { mutableStateOf("") }
    var overlayOutline by remember { mutableStateOf(true) }
    val selectedStickers = remember { mutableStateListOf<String>() }
    var frameStyle by remember { mutableStateOf("NONE") }
    var memeTopText by remember { mutableStateOf("") }
    var memeBottomText by remember { mutableStateOf("") }
    var holdToCompareOriginal by remember { mutableStateOf(false) }
    var compareSplitFraction by remember { mutableFloatStateOf(0.5f) }
    var enableSplitCompare by remember { mutableStateOf(false) }
    var outputFormat by remember { mutableStateOf(ImageOutputFormat.JPG) }
    var exportResult by remember { mutableStateOf<MediaExportResult?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaStudioEngine.decodeBitmapFromUri(context, uri, maxMegapixels = 12)?.let {
                    baseBitmap = it
                    exportResult = null
                }
            }
        }
    }

    val collagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        if (uris.size >= 2) {
            scope.launch {
                val decoded = uris.take(6).mapNotNull { u ->
                    MediaStudioEngine.decodeBitmapFromUri(context, u, maxMegapixels = 6)
                }
                if (decoded.isNotEmpty()) {
                    collageBitmaps = decoded
                    editorTab = "COLLAGE"
                }
            }
        }
    }

    val renderedPreview = remember(
        baseBitmap,
        collageBitmaps,
        editorTab,
        cropRatio,
        rotationDeg,
        flipH,
        flipV,
        straightenDeg,
        brightness,
        contrast,
        saturation,
        warmth,
        tint,
        highlights,
        shadows,
        sharpen,
        vignette,
        curvesStrength,
        grainAmount,
        fadeAmount,
        selectedFilter,
        mosaicPts.size,
        penStrokes.size,
        highlighterStrokes.size,
        overlayText,
        overlayOutline,
        selectedStickers.size,
        frameStyle,
        memeTopText,
        memeBottomText
    ) {
        if (editorTab == "COLLAGE" && collageBitmaps.isNotEmpty()) {
            MediaStudioEngine.renderPhotoCollage(collageBitmaps, canvasSizePx = 1080)
        } else {
            MediaStudioEngine.renderEditedPhoto(
                source = baseBitmap,
                cropAspectRatio = cropRatio,
                rotationDegrees = rotationDeg,
                flipHorizontal = flipH,
                flipVertical = flipV,
                straightenDegrees = straightenDeg,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                warmth = warmth,
                tint = tint,
                highlights = highlights,
                shadows = shadows,
                sharpen = sharpen,
                vignette = vignette,
                curvesStrength = curvesStrength,
                grainAmount = grainAmount,
                fadeAmount = fadeAmount,
                filterSpec = selectedFilter,
                mosaicPoints = mosaicPts.toList(),
                penPoints = penStrokes.toList(),
                highlighterPoints = highlighterStrokes.toList(),
                overlayText = overlayText,
                overlayTextColor = AndroidColor.WHITE,
                overlayTextOutline = overlayOutline,
                stickerIds = selectedStickers.toList(),
                frameStyle = frameStyle,
                memeTopText = memeTopText,
                memeBottomText = memeBottomText
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("photo_editor_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MediaSaveLocationTipBanner(prefs = prefs, preferencesRepository = preferencesRepository)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header + Undo / Redo / Hold to Compare
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Photo Editor Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                when {
                                    penStrokes.isNotEmpty() -> {
                                        penStrokes.removeAt(penStrokes.lastIndex)
                                        redoStack.add("PEN")
                                    }
                                    highlighterStrokes.isNotEmpty() -> {
                                        highlighterStrokes.removeAt(highlighterStrokes.lastIndex)
                                        redoStack.add("HIGHLIGHT")
                                    }
                                    mosaicPts.isNotEmpty() -> {
                                        mosaicPts.clear()
                                        redoStack.add("MOSAIC")
                                    }
                                    selectedStickers.isNotEmpty() -> {
                                        selectedStickers.removeAt(selectedStickers.lastIndex)
                                    }
                                    else -> {
                                        brightness = 0f
                                        contrast = 1f
                                        saturation = 1f
                                        selectedFilter = MediaStudioEngine.PHOTO_FILTERS.first()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("photo_editor_undo_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    redoStack.removeAt(redoStack.lastIndex)
                                }
                            },
                            modifier = Modifier.testTag("photo_editor_redo_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }
                    }
                }

                // Interactive Canvas (No ads on canvas, no watermark)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(275.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .pointerInput(editorTab, drawTool) {
                            if (editorTab == "DRAW_TEXT") {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val pt = PointF(
                                        (change.position.x / size.width.toFloat()).coerceIn(0f, 1f),
                                        (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    )
                                    when (drawTool) {
                                        "MOSAIC" -> mosaicPts.add(pt)
                                        "HIGHLIGHTER" -> {
                                            val prev = highlighterStrokes.lastOrNull()?.first.orEmpty()
                                            if (highlighterStrokes.isEmpty()) {
                                                highlighterStrokes.add(listOf(pt, pt) to AndroidColor.YELLOW)
                                            } else {
                                                highlighterStrokes[highlighterStrokes.lastIndex] =
                                                    (prev + pt) to AndroidColor.YELLOW
                                            }
                                        }
                                        else -> {
                                            val prev = penStrokes.lastOrNull()?.first.orEmpty()
                                            if (penStrokes.isEmpty()) {
                                                penStrokes.add(listOf(pt, pt) to AndroidColor.CYAN)
                                            } else {
                                                penStrokes[penStrokes.lastIndex] =
                                                    (prev + pt) to AndroidColor.CYAN
                                            }
                                        }
                                    }
                                }
                            } else {
                                detectTapGestures(
                                    onPress = {
                                        holdToCompareOriginal = true
                                        tryAwaitRelease()
                                        holdToCompareOriginal = false
                                    }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val showSplitCompare = enableSplitCompare || (selectedFilter.isPremium && !isEditorProUnlocked)
                    if (showSplitCompare && !holdToCompareOriginal) {
                        val splitPreviewBmp = remember(baseBitmap, renderedPreview, compareSplitFraction) {
                            val w = renderedPreview.width.coerceAtLeast(1)
                            val h = renderedPreview.height.coerceAtLeast(1)
                            val out = renderedPreview.copy(Bitmap.Config.ARGB_8888, true)
                            val scaledBase = if (baseBitmap.width == w && baseBitmap.height == h) {
                                baseBitmap
                            } else {
                                Bitmap.createScaledBitmap(baseBitmap, w, h, true)
                            }
                            val splitX = (w * compareSplitFraction).roundToInt().coerceIn(0, w)
                            val canvas = android.graphics.Canvas(out)
                            canvas.save()
                            canvas.clipRect(0, 0, splitX, h)
                            canvas.drawBitmap(scaledBase, 0f, 0f, null)
                            canvas.restore()
                            val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = AndroidColor.rgb(255, 215, 0)
                                strokeWidth = 4f
                            }
                            canvas.drawLine(splitX.toFloat(), 0f, splitX.toFloat(), h.toFloat(), linePaint)
                            out
                        }
                        Image(
                            bitmap = splitPreviewBmp.asImageBitmap(),
                            contentDescription = "Before and after compare preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "BEFORE (Original)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                text = "AFTER (${selectedFilter.name})",
                                style = MaterialTheme.typography.labelSmall,
                                color = DyneGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        val bmpToShow = if (holdToCompareOriginal) baseBitmap else renderedPreview
                        Image(
                            bitmap = bmpToShow.asImageBitmap(),
                            contentDescription = "Edited photo canvas",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = if (holdToCompareOriginal) {
                            "ORIGINAL"
                        } else if (selectedFilter.isPremium && !isEditorProUnlocked) {
                            "TRY BEFORE YOU BUY • Drag Before/After slider below"
                        } else {
                            "Hold canvas to compare"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (editorTab == "FILTERS" || (selectedFilter.isPremium && !isEditorProUnlocked)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DyneGold.copy(alpha = 0.10f))
                            .border(1.dp, DyneGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("photo_editor_before_after_slider_box"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Before / After Filter Compare (${(compareSplitFraction * 100).roundToInt()}%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = tokens.textPrimary
                            )
                            FilterChip(
                                selected = enableSplitCompare || (selectedFilter.isPremium && !isEditorProUnlocked),
                                onClick = { enableSplitCompare = !enableSplitCompare },
                                label = { Text(if (enableSplitCompare) "Split On" else "Split Preview") }
                            )
                        }
                        Slider(
                            value = compareSplitFraction,
                            onValueChange = {
                                enableSplitCompare = true
                                compareSplitFraction = it
                            },
                            valueRange = 0.05f..0.95f,
                            modifier = Modifier.testTag("photo_editor_compare_slider")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Photo")
                    }
                    OutlinedButton(
                        onClick = {
                            if (isEditorProUnlocked) {
                                collagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                featureGate?.require(
                                    feature = PremiumFeature.PHOTO_EDITOR_PRO,
                                    onUnlocked = {
                                        collagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Collage (2–6)")
                        if (!isEditorProUnlocked) {
                            Spacer(Modifier.width(4.dp))
                            ProBadge(text = "PRO")
                        }
                    }
                }

                // Tool Deck Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "CROP_ADJUST" to "Crop & Adjust",
                        "FILTERS" to "20 Filters",
                        "PRO_TONE" to "Curves & Grain",
                        "DRAW_TEXT" to "Draw, Text & Stickers",
                        "MEME_FRAME" to "Meme & Frames",
                        "COLLAGE" to "Collage"
                    ).forEach { (tabId, label) ->
                        FilterChip(
                            selected = editorTab == tabId,
                            onClick = {
                                if ((tabId == "PRO_TONE" || tabId == "COLLAGE") && !isEditorProUnlocked) {
                                    featureGate?.require(
                                        feature = PremiumFeature.PHOTO_EDITOR_PRO,
                                        onUnlocked = { editorTab = tabId }
                                    )
                                } else {
                                    editorTab = tabId
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                when (editorTab) {
                    "CROP_ADJUST" -> {
                        Text("Aspect Ratio & Rotate", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                null to "Free",
                                1f to "1:1",
                                0.8f to "4:5",
                                (16f / 9f) to "16:9",
                                (9f / 16f) to "9:16",
                                1.5f to "3:2"
                            ).forEach { (ratio, label) ->
                                FilterChip(
                                    selected = cropRatio == ratio,
                                    onClick = { cropRatio = ratio },
                                    label = { Text(label) }
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = { rotationDeg = (rotationDeg + 90) % 360 },
                                label = { Text("Rotate 90°") }
                            )
                            FilterChip(
                                selected = flipH,
                                onClick = { flipH = !flipH },
                                label = { Text("Flip H") }
                            )
                            FilterChip(
                                selected = flipV,
                                onClick = { flipV = !flipV },
                                label = { Text("Flip V") }
                            )
                        }

                        Text("Straighten: ${straightenDeg.roundToInt()}°", style = MaterialTheme.typography.bodySmall, color = tokens.textPrimary)
                        Slider(value = straightenDeg, onValueChange = { straightenDeg = it }, valueRange = -15f..15f)

                        Text("Brightness (${brightness.roundToInt()}) • Contrast (${String.format(Locale.US, "%.2f", contrast)})", style = MaterialTheme.typography.bodySmall, color = tokens.textPrimary)
                        Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -60f..60f)
                        Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.6f..1.6f)

                        Text("Saturation (${String.format(Locale.US, "%.2f", saturation)}) • Warmth (${warmth.roundToInt()})", style = MaterialTheme.typography.bodySmall, color = tokens.textPrimary)
                        Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..2f)
                        Slider(value = warmth, onValueChange = { warmth = it }, valueRange = -40f..40f)

                        Text("Vignette (${(vignette * 100).roundToInt()}%) • Shadows (${shadows.roundToInt()})", style = MaterialTheme.typography.bodySmall, color = tokens.textPrimary)
                        Slider(value = vignette, onValueChange = { vignette = it }, valueRange = 0f..0.9f)
                        Slider(value = shadows, onValueChange = { shadows = it }, valueRange = -35f..35f)
                    }

                    "FILTERS" -> {
                        Text(
                            "20 ColorMatrix Filters (6 Free, 14 Pro — Tap any Pro filter to compare Before/After)",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.textSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MediaStudioEngine.PHOTO_FILTERS.forEach { spec ->
                                FilterChip(
                                    selected = selectedFilter.id == spec.id,
                                    onClick = {
                                        selectedFilter = spec
                                        if (spec.isPremium && !isEditorProUnlocked) {
                                            enableSplitCompare = true
                                        }
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(spec.name)
                                            if (spec.isPremium && !isEditorProUnlocked) {
                                                ProBadge(text = "PRO")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        if (selectedFilter.isPremium && !isEditorProUnlocked) {
                            OutlinedButton(
                                onClick = {
                                    featureGate?.require(
                                        feature = PremiumFeature.PHOTO_EDITOR_PRO,
                                        onUnlocked = { enableSplitCompare = false }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Unlock ${selectedFilter.name} Filter for Full Export")
                                Spacer(Modifier.width(6.dp))
                                ProBadge(text = "PRO")
                            }
                        }
                    }

                    "PRO_TONE" -> {
                        Text("Tone Curves S-Contrast: ${String.format(Locale.US, "%+.2f", curvesStrength)}", style = MaterialTheme.typography.bodySmall, color = tokens.textPrimary)
                        Slider(value = curvesStrength, onValueChange = { curvesStrength = it }, valueRange = -0.8f..0.8f)

                        Text("Film Grain: ${(grainAmount * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = tokens.textPrimary)
                        Slider(value = grainAmount, onValueChange = { grainAmount = it }, valueRange = 0f..0.8f)

                        Text("Matte Fade: ${(fadeAmount * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = tokens.textPrimary)
                        Slider(value = fadeAmount, onValueChange = { fadeAmount = it }, valueRange = 0f..0.8f)
                    }

                    "DRAW_TEXT" -> {
                        Text("Brush Mode (Draw directly on canvas above)", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                        GlassSegmentedControl(
                            options = listOf("Pen", "Highlighter", "Mosaic Pixelate"),
                            selectedIndex = when (drawTool) {
                                "HIGHLIGHTER" -> 1
                                "MOSAIC" -> 2
                                else -> 0
                            },
                            onSelected = { idx ->
                                drawTool = when (idx) {
                                    1 -> "HIGHLIGHTER"
                                    2 -> "MOSAIC"
                                    else -> "PEN"
                                }
                            }
                        )
                        OutlinedTextField(
                            value = overlayText,
                            onValueChange = { overlayText = it },
                            label = { Text("Text Overlay") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Code-Drawn Vector Stickers", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("STAR", "HEART", "APPROVED", "CROWN", "SPARKLE").forEach { st ->
                                FilterChip(
                                    selected = st in selectedStickers,
                                    onClick = {
                                        if (st in selectedStickers) selectedStickers.remove(st) else selectedStickers.add(st)
                                    },
                                    label = { Text(st) }
                                )
                            }
                        }
                    }

                    "MEME_FRAME" -> {
                        OutlinedTextField(
                            value = memeTopText,
                            onValueChange = { memeTopText = it },
                            label = { Text("Meme Top Caption") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = memeBottomText,
                            onValueChange = { memeBottomText = it },
                            label = { Text("Meme Bottom Caption") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Decorative Frame", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("NONE", "POLAROID", "CINEMA", "NEON", "MUSEUM", "VINTAGE").forEach { fr ->
                                FilterChip(
                                    selected = frameStyle == fr,
                                    onClick = { frameStyle = fr },
                                    label = { Text(fr) }
                                )
                            }
                        }
                    }

                    "COLLAGE" -> {
                        Text(
                            text = "2–6 Photo Grid Collage (${maxOf(2, collageBitmaps.size)} photos)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textPrimary
                        )
                        Button(
                            onClick = {
                                collagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        ) {
                            Text("Select 2–6 Photos for Collage")
                        }
                    }
                }

                // Export Format & Save Button
                GlassSegmentedControl(
                    options = ImageOutputFormat.entries.map { "Export ${it.label}" },
                    selectedIndex = outputFormat.ordinal,
                    onSelected = { idx -> outputFormat = ImageOutputFormat.entries[idx] }
                )

                Button(
                    onClick = {
                        val exportNow: () -> Unit = {
                            scope.launch {
                                val outDir = File(context.cacheDir, "media_studio/photo_editor").apply { mkdirs() }
                                val outFile = File(outDir, "Dyne_Edited_${System.currentTimeMillis()}.${outputFormat.ext}")
                                FileOutputStream(outFile).use { out ->
                                    renderedPreview.compress(outputFormat.toCompressFormat(), 94, out)
                                }
                                val (savedPath, savedUri) = MediaStudioEngine.saveFileToMediaStore(
                                    context, outFile, outputFormat.mimeType, MediaStudioEngine.MediaStoreCategory.IMAGE
                                )
                                val res = MediaExportResult(
                                    toolId = "photo_editor",
                                    toolName = "Photo Editor",
                                    fileName = outFile.name,
                                    mimeType = outputFormat.mimeType,
                                    localCacheFile = outFile,
                                    savedMediaStorePath = savedPath,
                                    savedMediaStoreUri = savedUri,
                                    beforeSizeBytes = MediaStudioEngine.estimateCompressedImageBytes(baseBitmap, outputFormat, 92),
                                    afterSizeBytes = outFile.length(),
                                    beforePreviewBitmap = baseBitmap,
                                    afterPreviewBitmap = renderedPreview,
                                    summaryText = "${selectedFilter.name} • ${renderedPreview.width}×${renderedPreview.height} px • Zero watermark"
                                )
                                exportResult = res
                                recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                            }
                        }
                        if (selectedFilter.isPremium && !isEditorProUnlocked && featureGate != null) {
                            featureGate.require(
                                feature = PremiumFeature.PHOTO_EDITOR_PRO,
                                onUnlocked = exportNow
                            )
                        } else {
                            exportNow()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_editor_export_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export Edited Photo (${outputFormat.label})")
                }
            }
        }

        MediaExportResultCard(
            result = exportResult,
            onConvertAnother = { exportResult = null },
            onToast = { statusBanner = it }
        )

        MediaStudioRecentFilesSection(
            scanViewModel = scanViewModel,
            onToast = { statusBanner = it }
        )
    }
}

// ============================================================================
// 4. PASSPORT & ID PHOTO MAKER (Premium, 1 free try; 300 DPI print sheet)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassportIdPhotoToolCard(
    activeModeId: String,
    isPro: Boolean,
    prefs: UserPreferences,
    preferencesRepository: PreferencesRepository?,
    scanViewModel: ScanViewModel?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGlassTokens.current
    val featureGate = LocalFeatureGate.current
    val adManager = LocalAdManager.current
    val isPassportProUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.PASSPORT_ID_PHOTO) == true)

    var sourceBitmap by remember { mutableStateOf(MediaStudioEngine.createSampleStudioBitmap(portraitSubject = true)) }
    var selectedPreset by remember { mutableStateOf(PassportPreset.US_2X2) }
    var backdropColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var printSheetSize by remember(activeModeId) {
        mutableStateOf(if (activeModeId == "print_sheet") PrintSheetSize.SHEET_4X6 else PrintSheetSize.NONE)
    }
    var zoomFactor by remember { mutableFloatStateOf(1f) }
    var isProcessing by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<MediaExportResult?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaStudioEngine.decodeBitmapFromUri(context, uri, maxMegapixels = 12)?.let {
                    sourceBitmap = it
                    exportResult = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("passport_id_photo_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MediaSaveLocationTipBanner(prefs = prefs, preferencesRepository = preferencesRepository)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Passport & ID Photo Maker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = selectedPreset.title + " • " + selectedPreset.dimensionsLabel + " (300 DPI)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isPassportProUnlocked) {
                        ProBadge(text = "PRO UNLOCKED")
                    } else if (!prefs.passportPhotoFreeTryUsed) {
                        ProBadge(text = "1 FREE TRY")
                    } else {
                        ProBadge(text = "PRO")
                    }
                }

                // Mandatory compliance disclaimer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DyneGold.copy(alpha = 0.14f))
                        .border(1.dp, DyneGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .testTag("passport_photo_disclaimer_box")
                ) {
                    Text(
                        text = "Check your country's official requirements; acceptance is not guaranteed.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary
                    )
                }

                // Face-Guide Biometric Overlay Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(backdropColor))
                        .border(1.dp, tokens.cardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = sourceBitmap.asImageBitmap(),
                        contentDescription = "Passport photo preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Face-guide oval + eye-line + crown/chin markers
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.46f
                        val rx = size.width * 0.24f
                        val ry = size.height * 0.32f
                        drawOval(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(cx - rx, cy - ry),
                            size = Size(rx * 2f, ry * 2f),
                            style = Stroke(width = 3f)
                        )
                        // Eye level guide line
                        drawLine(
                            color = Color(0xFFFFCC00),
                            start = Offset(cx - rx * 1.25f, cy - ry * 0.12f),
                            end = Offset(cx + rx * 1.25f, cy - ry * 0.12f),
                            strokeWidth = 2f
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pick Portrait Photo")
                }

                // Country Size Presets
                Text("Official Country Size Preset", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PassportPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset },
                            label = { Text("${preset.title} (${preset.dimensionsLabel})") }
                        )
                    }
                }

                // Background Color (White or Official Light Blue)
                Text("Official Backdrop Color", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                GlassSegmentedControl(
                    options = listOf("White (#FFFFFF)", "Blue (#D6EAF8)", "Light Gray"),
                    selectedIndex = when (backdropColor) {
                        AndroidColor.rgb(214, 234, 248) -> 1
                        AndroidColor.rgb(242, 244, 244) -> 2
                        else -> 0
                    },
                    onSelected = { idx ->
                        backdropColor = when (idx) {
                            1 -> AndroidColor.rgb(214, 234, 248)
                            2 -> AndroidColor.rgb(242, 244, 244)
                            else -> AndroidColor.WHITE
                        }
                    }
                )

                // Print Sheet Selector (Single, 4x6 in at 300 DPI, A4 at 300 DPI)
                Text("300 DPI Print Sheet Layout", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                GlassSegmentedControl(
                    options = listOf("Single ID", "4×6 in (300 DPI)", "A4 Sheet (300 DPI)"),
                    selectedIndex = printSheetSize.ordinal,
                    onSelected = { idx -> printSheetSize = PrintSheetSize.entries[idx] }
                )

                Text("Head Scale / Zoom: ${(zoomFactor * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = tokens.textPrimary)
                Slider(value = zoomFactor, onValueChange = { zoomFactor = it }, valueRange = 0.85f..1.35f)

                Button(
                    onClick = {
                        scope.launch {
                            val allowed = if (isPassportProUnlocked) {
                                true
                            } else {
                                preferencesRepository?.consumePassportPhotoFreeTry() ?: true
                            }
                            if (!allowed) {
                                featureGate?.require(
                                    feature = PremiumFeature.PASSPORT_ID_PHOTO,
                                    onUnlocked = {}
                                )
                                return@launch
                            }

                            isProcessing = true
                            try {
                                val finalBmp = MediaStudioEngine.renderPassportOrPrintSheet(
                                    source = sourceBitmap,
                                    preset = selectedPreset,
                                    backdropColorInt = backdropColor,
                                    printSheetSize = printSheetSize,
                                    zoomFactor = zoomFactor
                                )
                                val outDir = File(context.cacheDir, "media_studio/passport").apply { mkdirs() }
                                val outFile = File(outDir, "Dyne_Passport_${selectedPreset.name}_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(outFile).use { finalBmp.compress(Bitmap.CompressFormat.JPEG, 96, it) }
                                val (savedPath, savedUri) = MediaStudioEngine.saveFileToMediaStore(
                                    context, outFile, "image/jpeg", MediaStudioEngine.MediaStoreCategory.IMAGE
                                )
                                val res = MediaExportResult(
                                    toolId = "passport_id_photo",
                                    toolName = "Passport & ID Photo",
                                    fileName = outFile.name,
                                    mimeType = "image/jpeg",
                                    localCacheFile = outFile,
                                    savedMediaStorePath = savedPath,
                                    savedMediaStoreUri = savedUri,
                                    beforeSizeBytes = MediaStudioEngine.estimateCompressedImageBytes(sourceBitmap, ImageOutputFormat.JPG, 92),
                                    afterSizeBytes = outFile.length(),
                                    beforePreviewBitmap = sourceBitmap,
                                    afterPreviewBitmap = finalBmp,
                                    summaryText = "${selectedPreset.title} (${selectedPreset.dimensionsLabel}) • ${printSheetSize.label} • 300 DPI"
                                )
                                exportResult = res
                                recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("passport_photo_generate_btn")
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (!isPassportProUnlocked && !prefs.passportPhotoFreeTryUsed) {
                            "Generate 300 DPI ID Photo (Use 1 Free Try)"
                        } else {
                            "Generate 300 DPI ID Photo"
                        }
                    )
                }
            }
        }

        MediaExportResultCard(
            result = exportResult,
            onConvertAnother = { exportResult = null },
            onToast = { statusBanner = it }
        )

        MediaStudioRecentFilesSection(
            scanViewModel = scanViewModel,
            onToast = { statusBanner = it }
        )
    }
}

// ============================================================================
// 5. AUDIO CONVERTER & STUDIO (WAV, M4A/AAC, device FLAC/Opus; MP3 input note)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioConverterToolCard(
    activeModeId: String,
    isPro: Boolean,
    prefs: UserPreferences,
    preferencesRepository: PreferencesRepository?,
    scanViewModel: ScanViewModel?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGlassTokens.current
    val featureGate = LocalFeatureGate.current
    val adManager = LocalAdManager.current
    val isAudioProUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.AUDIO_CONVERTER_PRO) == true)

    val caps = remember(context) { DeviceCapabilities.initialize(context) }
    val availableFormats = remember(caps) {
        buildList {
            add(AudioOutputFormat.WAV)
            if (caps.hasAacEncoder) add(AudioOutputFormat.M4A)
            if (caps.hasFlacEncoder) add(AudioOutputFormat.FLAC)
            if (caps.hasOpusEncoder) add(AudioOutputFormat.OPUS)
        }
    }

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var outputFormat by remember { mutableStateOf(AudioOutputFormat.WAV) }
    var sampleRateHz by remember { mutableIntStateOf(44100) }
    var channels by remember { mutableIntStateOf(2) }
    var bitrateKbps by remember { mutableIntStateOf(192) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    var fadeInOut by remember { mutableStateOf(false) }
    var normalizePeak by remember { mutableStateOf(true) }
    var speedFactor by remember { mutableFloatStateOf(1f) }
    var saveAsRingtone by remember(activeModeId) { mutableStateOf(activeModeId == "cutter_ringtone") }
    var mergeAll by remember(activeModeId) { mutableStateOf(activeModeId == "merge") }

    var activeJob by remember { mutableStateOf<Job?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var exportResult by remember { mutableStateOf<MediaExportResult?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val audioSafPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = if (isAudioProUnlocked || mergeAll) uris else uris.take(1)
            exportResult = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_converter_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MediaSaveLocationTipBanner(prefs = prefs, preferencesRepository = preferencesRepository)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Converter, Cutter & Ringtone Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Input: MP3, M4A/AAC, WAV, OGG, FLAC, AMR, 3GP or Video. MP3 is supported as input.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isAudioProUnlocked) {
                        ProBadge(text = "PRO UNLIMITED")
                    } else {
                        QuotaAllowanceChip(
                            quotaType = QuotaType.AUDIO_CONVERT,
                            isProUnlocked = false,
                            prefs = prefs,
                            onClick = { featureGate?.openQuotaUnlockSheet(QuotaType.AUDIO_CONVERT) }
                        )
                    }
                }

                Button(
                    onClick = {
                        audioSafPicker.launch(arrayOf("audio/*", "video/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_converter_pick_btn")
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedUris.isEmpty()) "Select Audio or Video File(s)"
                        else "Selected ${selectedUris.size} File(s) (${MediaStudioEngine.resolveDisplayName(context, selectedUris.first())})"
                    )
                }

                // Waveform Trim Visualizer Canvas
                Text(
                    text = "Waveform Trim (${(trimStart * 100).roundToInt()}% – ${(trimEnd * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.textSecondary
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.cardBackground.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    val bars = 48
                    val barW = size.width / (bars * 1.5f)
                    for (i in 0 until bars) {
                        val pos = i.toFloat() / bars.toFloat()
                        val inTrim = pos in trimStart..trimEnd
                        val amp = (0.25f + 0.70f * kotlin.math.abs(kotlin.math.sin(i * 0.42f)))
                        val h = size.height * amp
                        drawRoundRect(
                            color = if (inTrim) Color(0xFF00E5FF) else Color.Gray.copy(alpha = 0.35f),
                            topLeft = Offset(i * (size.width / bars), (size.height - h) * 0.5f),
                            size = Size(barW, h)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start: ${(trimStart * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                        Slider(value = trimStart, onValueChange = { trimStart = it.coerceAtMost(trimEnd - 0.05f) }, valueRange = 0f..0.9f)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End: ${(trimEnd * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                        Slider(value = trimEnd, onValueChange = { trimEnd = it.coerceAtLeast(trimStart + 0.05f) }, valueRange = 0.1f..1f)
                    }
                }

                // Output Format (only formats whose hardware encoder exists!)
                Text("Output Format (MP3 is supported as input)", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableFormats.forEach { fmt ->
                        FilterChip(
                            selected = outputFormat == fmt,
                            onClick = { outputFormat = fmt },
                            label = { Text(fmt.label) }
                        )
                    }
                }

                // Sample Rate, Channels & Bitrate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(22050, 44100, 48000).forEach { sr ->
                        FilterChip(
                            selected = sampleRateHz == sr,
                            onClick = { sampleRateHz = sr },
                            label = { Text("${sr / 1000.0} kHz") }
                        )
                    }
                    FilterChip(
                        selected = channels == 1,
                        onClick = { channels = if (channels == 1) 2 else 1 },
                        label = { Text(if (channels == 1) "Mono" else "Stereo") }
                    )
                }

                // Fade, Normalize, Ringtone Maker, Merge (Pro), Speed/Pitch (Pro)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fade In / Out & Peak Normalize", style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
                    Switch(checked = fadeInOut, onCheckedChange = { fadeInOut = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Save to Ringtones (Ringtone Maker)", style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
                    Switch(checked = saveAsRingtone, onCheckedChange = { saveAsRingtone = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Merge Multiple Audio Tracks", style = MaterialTheme.typography.bodyMedium, color = tokens.textPrimary)
                        if (!isAudioProUnlocked) ProBadge(text = "PRO")
                    }
                    Switch(
                        checked = mergeAll && isAudioProUnlocked,
                        onCheckedChange = { want ->
                            if (want && !isAudioProUnlocked) {
                                featureGate?.require(
                                    feature = PremiumFeature.AUDIO_CONVERTER_PRO,
                                    onUnlocked = { mergeAll = true }
                                )
                            } else {
                                mergeAll = want
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speed / Pitch: ${String.format(Locale.US, "%.2fx", speedFactor)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textPrimary
                    )
                    if (!isAudioProUnlocked) ProBadge(text = "PRO")
                }
                Slider(
                    value = speedFactor,
                    onValueChange = { v ->
                        if (!isAudioProUnlocked) {
                            featureGate?.require(
                                feature = PremiumFeature.AUDIO_CONVERTER_PRO,
                                onUnlocked = { speedFactor = v }
                            )
                        } else {
                            speedFactor = v
                        }
                    },
                    valueRange = 0.5f..2.0f
                )

                Button(
                    onClick = {
                        val runAudioConvert: () -> Unit = {
                            activeJob?.cancel()
                            exportResult = null
                            isProcessing = true
                            activeJob = scope.launch {
                                try {
                                    val res = MediaStudioEngine.processAudioConversion(
                                        context = context,
                                        uris = selectedUris,
                                        outputFormat = outputFormat,
                                        sampleRateHz = sampleRateHz,
                                        channels = channels,
                                        bitrateKbps = bitrateKbps,
                                        trimStartRatio = trimStart,
                                        trimEndRatio = trimEnd,
                                        fadeInOut = fadeInOut,
                                        normalizePeak = normalizePeak,
                                        speedFactor = speedFactor,
                                        saveAsRingtone = saveAsRingtone,
                                        mergeAllInputs = mergeAll,
                                        isProUnlocked = isAudioProUnlocked,
                                        onProgress = { _, _, msg -> progressStatus = msg }
                                    )
                                    exportResult = res
                                    recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                                } catch (_: CancellationException) {
                                    statusBanner = "Audio conversion cancelled."
                                } catch (e: Exception) {
                                    statusBanner = "Error: ${e.message ?: "Unsupported audio format."}"
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                        if (isAudioProUnlocked || featureGate == null) {
                            runAudioConvert()
                        } else {
                            featureGate.requireQuotaOrUnlock(
                                quotaType = QuotaType.AUDIO_CONVERT,
                                onAllowed = runAudioConvert
                            )
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_converter_run_btn")
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Convert Audio to ${outputFormat.label}")
                }
            }
        }

        MediaProcessingProgressCard(
            isProcessing = isProcessing,
            progressCurrent = 1,
            progressTotal = 2,
            statusText = progressStatus,
            onCancel = {
                activeJob?.cancel()
                isProcessing = false
            }
        )

        MediaExportResultCard(
            result = exportResult,
            onConvertAnother = { exportResult = null },
            onToast = { statusBanner = it }
        )

        MediaStudioRecentFilesSection(
            scanViewModel = scanViewModel,
            onToast = { statusBanner = it }
        )
    }
}

// ============================================================================
// 6. VIDEO TOOLS (Trim, Extract Audio, Mute Free; Compress & Resize Pro via Media3)
// ============================================================================
@Composable
fun VideoToolsCard(
    activeModeId: String,
    isPro: Boolean,
    prefs: UserPreferences,
    preferencesRepository: PreferencesRepository?,
    scanViewModel: ScanViewModel?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGlassTokens.current
    val featureGate = LocalFeatureGate.current
    val adManager = LocalAdManager.current
    val isVideoProUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.VIDEO_COMPRESS_PRO) == true)
    val hasMedia3 = remember { DeviceCapabilities.hasMedia3TransformerClass() }

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoMode by remember(activeModeId) {
        mutableStateOf(
            when (activeModeId) {
                "mute" -> "MUTE"
                "extract_audio" -> "EXTRACT_AUDIO"
                "compress_resize" -> "COMPRESS_RESIZE"
                else -> "TRIM"
            }
        )
    }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    var targetResHeight by remember { mutableIntStateOf(720) }

    var activeJob by remember { mutableStateOf<Job?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var exportResult by remember { mutableStateOf<MediaExportResult?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            exportResult = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_tools_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MediaSaveLocationTipBanner(prefs = prefs, preferencesRepository = preferencesRepository)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Video Tools (MediaExtractor / MediaMuxer & Media3)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )

                Button(
                    onClick = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_tools_pick_btn")
                ) {
                    Text(
                        if (videoUri == null) "Pick Video from Gallery"
                        else "Selected: ${MediaStudioEngine.resolveDisplayName(context, videoUri!!)}"
                    )
                }

                GlassSegmentedControl(
                    options = buildList {
                        add("Trim")
                        add("Mute")
                        add("Extract Audio")
                        if (hasMedia3) add("Compress (Pro)")
                    },
                    selectedIndex = when (videoMode) {
                        "MUTE" -> 1
                        "EXTRACT_AUDIO" -> 2
                        "COMPRESS_RESIZE" -> 3
                        else -> 0
                    }.coerceAtMost(if (hasMedia3) 3 else 2),
                    onSelected = { idx ->
                        val next = when (idx) {
                            1 -> "MUTE"
                            2 -> "EXTRACT_AUDIO"
                            3 -> "COMPRESS_RESIZE"
                            else -> "TRIM"
                        }
                        if (next == "COMPRESS_RESIZE" && !isVideoProUnlocked) {
                            featureGate?.require(
                                feature = PremiumFeature.VIDEO_COMPRESS_PRO,
                                onUnlocked = { videoMode = "COMPRESS_RESIZE" }
                            )
                        } else {
                            videoMode = next
                        }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start: ${(trimStart * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                        Slider(value = trimStart, onValueChange = { trimStart = it.coerceAtMost(trimEnd - 0.05f) })
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End: ${(trimEnd * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                        Slider(value = trimEnd, onValueChange = { trimEnd = it.coerceAtLeast(trimStart + 0.05f) })
                    }
                }

                if (videoMode == "COMPRESS_RESIZE") {
                    Text("Target Resolution (Media3 Transformer)", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                    GlassSegmentedControl(
                        options = listOf("1080p Full HD", "720p HD", "480p Compact"),
                        selectedIndex = when (targetResHeight) {
                            1080 -> 0
                            480 -> 2
                            else -> 1
                        },
                        onSelected = { idx ->
                            targetResHeight = when (idx) {
                                0 -> 1080
                                2 -> 480
                                else -> 720
                            }
                        }
                    )
                }

                if (!hasMedia3) {
                    Text(
                        text = "Compress & Resize (Premium) is hidden because the Media3 Transformer dependency is not enabled in this build. Lossless Trim, Mute, and Extract Audio run natively via MediaExtractor/MediaMuxer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary
                    )
                }

                Button(
                    onClick = {
                        activeJob?.cancel()
                        exportResult = null
                        isProcessing = true
                        activeJob = scope.launch {
                            try {
                                val res = MediaStudioEngine.processVideoToolAction(
                                    context = context,
                                    videoUri = videoUri,
                                    mode = videoMode,
                                    trimStartRatio = trimStart,
                                    trimEndRatio = trimEnd,
                                    targetResolutionHeight = targetResHeight,
                                    onProgress = { _, _, msg -> progressStatus = msg }
                                )
                                exportResult = res
                                recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                            } catch (_: CancellationException) {
                                statusBanner = "Video processing cancelled."
                            } catch (e: Exception) {
                                statusBanner = "Error: ${e.message ?: "Unsupported video format."}"
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_tools_run_btn")
                ) {
                    Text(
                        when (videoMode) {
                            "MUTE" -> "Mute Video & Export MP4"
                            "EXTRACT_AUDIO" -> "Extract Audio Track to M4A"
                            "COMPRESS_RESIZE" -> "Compress & Resize Video (${targetResHeight}p)"
                            else -> "Trim Video & Export MP4"
                        }
                    )
                }

                statusBanner?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        MediaProcessingProgressCard(
            isProcessing = isProcessing,
            progressCurrent = 1,
            progressTotal = 2,
            statusText = progressStatus,
            onCancel = {
                activeJob?.cancel()
                isProcessing = false
            }
        )

        MediaExportResultCard(
            result = exportResult,
            onConvertAnother = { exportResult = null },
            onToast = { statusBanner = it }
        )

        MediaStudioRecentFilesSection(
            scanViewModel = scanViewModel,
            onToast = { statusBanner = it }
        )
    }
}

// ============================================================================
// 7. PDF & IMAGE STUDIO (Images to PDF, PDF to JPG/PNG, Bulk Compress, EXIF, Base64, Palette, Social)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfAndImageStudioCard(
    activeModeId: String,
    isPro: Boolean,
    prefs: UserPreferences,
    preferencesRepository: PreferencesRepository?,
    scanViewModel: ScanViewModel?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGlassTokens.current
    val featureGate = LocalFeatureGate.current
    val adManager = LocalAdManager.current
    val isPdfImageProUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.PDF_IMAGE_PRO) == true)

    var subMode by remember(activeModeId) {
        mutableStateOf(
            when (activeModeId) {
                "pdf_to_img" -> "PDF_TO_IMG"
                "exif_clean" -> "EXIF"
                "palette" -> "PALETTE"
                "social_resize" -> "SOCIAL"
                "base64" -> "BASE64"
                "bulk_compress" -> "BULK_COMPRESS"
                else -> "IMG_TO_PDF"
            }
        )
    }

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPageSize by remember { mutableStateOf(PdfPageSize.A4) }
    var pdfOutFormat by remember { mutableStateOf(ImageOutputFormat.JPG) }
    var exifItems by remember { mutableStateOf<List<ExifMetadataItem>>(emptyList()) }
    var paletteSwatches by remember { mutableStateOf<List<SwatchColorItem>>(emptyList()) }
    var sampleBmp by remember { mutableStateOf(MediaStudioEngine.createSampleStudioBitmap()) }
    var base64Text by remember { mutableStateOf("") }
    var selectedSocialPreset by remember { mutableStateOf(SocialResizePreset.IG_SQUARE) }

    var activeJob by remember { mutableStateOf<Job?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressCurrent by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(1) }
    var progressStatus by remember { mutableStateOf("") }
    var exportResult by remember { mutableStateOf<MediaExportResult?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris
            exportResult = null
            scope.launch {
                val first = uris.first()
                MediaStudioEngine.decodeBitmapFromUri(context, first, maxMegapixels = 12)?.let { bmp ->
                    sampleBmp = bmp
                    paletteSwatches = MediaStudioEngine.extractColorPaletteFromBitmap(bmp)
                }
                exifItems = MediaStudioEngine.inspectExifMetadata(context, first)
            }
        }
    }

    val pdfDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            exportResult = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_image_studio_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MediaSaveLocationTipBanner(prefs = prefs, preferencesRepository = preferencesRepository)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PDF & Image Studio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )

                // Sub-tool Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "IMG_TO_PDF" to "Images to PDF",
                        "PDF_TO_IMG" to "PDF to JPG/PNG",
                        "BULK_COMPRESS" to "Bulk Compress (Pro)",
                        "EXIF" to "EXIF Viewer & Cleaner",
                        "PALETTE" to "Color Palette",
                        "SOCIAL" to "Social Resize",
                        "BASE64" to "Base64 Codec"
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = subMode == id,
                            onClick = {
                                if (id == "BULK_COMPRESS" && !isPdfImageProUnlocked) {
                                    featureGate?.require(
                                        feature = PremiumFeature.PDF_IMAGE_PRO,
                                        onUnlocked = { subMode = id }
                                    )
                                } else {
                                    subMode = id
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                when (subMode) {
                    "IMG_TO_PDF" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Page Size Selector", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                            QuotaAllowanceChip(
                                quotaType = QuotaType.PDF_EXPORT,
                                isProUnlocked = isPdfImageProUnlocked,
                                prefs = prefs,
                                onClick = { featureGate?.openQuotaUnlockSheet(QuotaType.PDF_EXPORT) }
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(PdfPageSize.AUTO, PdfPageSize.A4, PdfPageSize.US_LETTER, PdfPageSize.LEGAL, PdfPageSize.A5).forEach { ps ->
                                FilterChip(
                                    selected = selectedPageSize == ps,
                                    onClick = { selectedPageSize = ps },
                                    label = { Text(ps.label) }
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                multiImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (selectedImageUris.isEmpty()) "Select Images for PDF" else "Selected ${selectedImageUris.size} Image(s)")
                        }
                        Button(
                            onClick = {
                                val runPdfBuild: () -> Unit = {
                                    activeJob?.cancel()
                                    isProcessing = true
                                    activeJob = scope.launch {
                                        try {
                                            val res = MediaStudioEngine.convertImagesToPdf(
                                                context = context,
                                                uris = selectedImageUris,
                                                pageSize = selectedPageSize,
                                                onProgress = { cur, tot, msg ->
                                                    progressCurrent = cur
                                                    progressTotal = tot
                                                    progressStatus = msg
                                                }
                                            )
                                            exportResult = res
                                            recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                }
                                if (isPdfImageProUnlocked || featureGate == null) {
                                    runPdfBuild()
                                } else {
                                    featureGate.requireQuotaOrUnlock(
                                        quotaType = QuotaType.PDF_EXPORT,
                                        onAllowed = runPdfBuild
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("img_to_pdf_run_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Build PDF (${selectedPageSize.label})")
                        }
                    }

                    "PDF_TO_IMG" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPdfImageProUnlocked) "Unlimited PDF Pages to JPG/PNG" else "Free up to 10 pages (Pro unlimited)",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.textSecondary
                            )
                            if (!isPdfImageProUnlocked) ProBadge(text = "FREE ≤10P")
                        }
                        OutlinedButton(
                            onClick = { pdfDocPicker.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (selectedPdfUri == null) "Select PDF Document"
                                else "PDF: ${MediaStudioEngine.resolveDisplayName(context, selectedPdfUri!!)}"
                            )
                        }
                        GlassSegmentedControl(
                            options = listOf("JPG", "PNG"),
                            selectedIndex = if (pdfOutFormat == ImageOutputFormat.PNG) 1 else 0,
                            onSelected = { idx -> pdfOutFormat = if (idx == 1) ImageOutputFormat.PNG else ImageOutputFormat.JPG }
                        )
                        Button(
                            onClick = {
                                activeJob?.cancel()
                                isProcessing = true
                                activeJob = scope.launch {
                                    try {
                                        val res = MediaStudioEngine.convertPdfToImages(
                                            context = context,
                                            pdfUri = selectedPdfUri,
                                            outputFormat = pdfOutFormat,
                                            isProUnlocked = isPdfImageProUnlocked,
                                            onProgress = { cur, tot, msg ->
                                                progressCurrent = cur
                                                progressTotal = tot
                                                progressStatus = msg
                                            }
                                        )
                                        exportResult = res
                                        recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_to_img_run_btn")
                        ) {
                            Text("Render PDF Pages to ${pdfOutFormat.label}")
                        }
                    }

                    "BULK_COMPRESS" -> {
                        OutlinedButton(
                            onClick = {
                                multiImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pick Images for Bulk Compress (${selectedImageUris.size} selected)")
                        }
                        Button(
                            onClick = {
                                activeJob?.cancel()
                                isProcessing = true
                                activeJob = scope.launch {
                                    try {
                                        val res = MediaStudioEngine.convertPhotosBatch(
                                            context = context,
                                            uris = selectedImageUris,
                                            sampleFallbackBitmap = sampleBmp,
                                            outputFormat = ImageOutputFormat.JPG,
                                            quality = 68,
                                            resizeMode = PhotoResizeMode.FHD_1080P,
                                            resizePercent = 100,
                                            exactWidth = 1920,
                                            exactHeight = 1080,
                                            keepExif = false,
                                            renamePattern = "{name}_compressed_{index}",
                                            isProUnlocked = true,
                                            onProgress = { cur, tot, msg ->
                                                progressCurrent = cur
                                                progressTotal = tot
                                                progressStatus = msg
                                            }
                                        )
                                        exportResult = res
                                        recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Run Bulk Image Compress")
                        }
                    }

                    "EXIF" -> {
                        OutlinedButton(
                            onClick = {
                                multiImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pick Photo to Inspect / Clean EXIF")
                        }
                        if (exifItems.isEmpty()) {
                            Text("Camera Make, Model, ISO, Shutter, and GPS tags will appear here.", style = MaterialTheme.typography.bodySmall, color = tokens.textSecondary)
                        } else {
                            exifItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.label, style = MaterialTheme.typography.bodySmall, color = tokens.textSecondary)
                                    Text(item.value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = tokens.textPrimary)
                                }
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val res = MediaStudioEngine.convertPhotosBatch(
                                        context = context,
                                        uris = selectedImageUris.take(1),
                                        sampleFallbackBitmap = sampleBmp,
                                        outputFormat = ImageOutputFormat.JPG,
                                        quality = 94,
                                        resizeMode = PhotoResizeMode.ORIGINAL,
                                        resizePercent = 100,
                                        exactWidth = sampleBmp.width,
                                        exactHeight = sampleBmp.height,
                                        keepExif = false,
                                        renamePattern = "{name}_clean_no_exif",
                                        isProUnlocked = true,
                                        onProgress = { _, _, _ -> }
                                    )
                                    exportResult = res
                                    recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exif_clean_export_btn")
                        ) {
                            Text("Clean & Strip All EXIF / GPS Metadata")
                        }
                    }

                    "PALETTE" -> {
                        val swatches = remember(sampleBmp, paletteSwatches) {
                            paletteSwatches.ifEmpty {
                                listOf(
                                    SwatchColorItem("Dominant", AndroidColor.rgb(24, 42, 78), "#182A4E", "RGB(24, 42, 78)", 34),
                                    SwatchColorItem("Vibrant", AndroidColor.rgb(212, 82, 110), "#D4526E", "RGB(212, 82, 110)", 24),
                                    SwatchColorItem("Accent", AndroidColor.rgb(42, 157, 143), "#2A9D8F", "RGB(42, 157, 143)", 18),
                                    SwatchColorItem("Warm Glow", AndroidColor.rgb(255, 214, 102), "#FFD666", "RGB(255, 214, 102)", 14)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                multiImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pick Photo to Extract Color Palette")
                        }
                        swatches.forEach { sw ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tokens.cardBackground.copy(alpha = 0.5f))
                                    .clickable {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("Color HEX", sw.hex))
                                        statusBanner = "Copied ${sw.hex} to clipboard"
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(sw.colorInt))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${sw.label} • ${sw.hex}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
                                    Text("${sw.rgbLabel} (${sw.populationPercent}%)", style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                                }
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy HEX", tint = tokens.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    "SOCIAL" -> {
                        Text("Social Resize Presets (Center-Crop & Export)", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SocialResizePreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = selectedSocialPreset == preset,
                                    onClick = { selectedSocialPreset = preset },
                                    label = { Text("${preset.title} (${preset.widthPx}×${preset.heightPx})") }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val cropped = MediaStudioEngine.centerCropBitmap(
                                        sampleBmp,
                                        selectedSocialPreset.widthPx,
                                        selectedSocialPreset.heightPx
                                    )
                                    val outDir = File(context.cacheDir, "media_studio/social").apply { mkdirs() }
                                    val outFile = File(outDir, "Dyne_${selectedSocialPreset.name}_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(outFile).use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                                    val (savedPath, savedUri) = MediaStudioEngine.saveFileToMediaStore(
                                        context, outFile, "image/jpeg", MediaStudioEngine.MediaStoreCategory.IMAGE
                                    )
                                    val res = MediaExportResult(
                                        toolId = "pdf_image_tools",
                                        toolName = "Social Resize",
                                        fileName = outFile.name,
                                        mimeType = "image/jpeg",
                                        localCacheFile = outFile,
                                        savedMediaStorePath = savedPath,
                                        savedMediaStoreUri = savedUri,
                                        beforeSizeBytes = MediaStudioEngine.estimateCompressedImageBytes(sampleBmp, ImageOutputFormat.JPG, 92),
                                        afterSizeBytes = outFile.length(),
                                        beforePreviewBitmap = sampleBmp,
                                        afterPreviewBitmap = cropped,
                                        summaryText = "${selectedSocialPreset.title} (${selectedSocialPreset.subtitle})"
                                    )
                                    exportResult = res
                                    recordAndNotifyFinishedExport(context, scanViewModel, adManager, res)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export ${selectedSocialPreset.title}")
                        }
                    }

                    "BASE64" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        base64Text = MediaStudioEngine.encodeBitmapToBase64(sampleBmp, quality = 75)
                                        statusBanner = "Encoded image to Base64 (${base64Text.length} chars)"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Image → Base64")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val decoded = MediaStudioEngine.decodeBase64ToBitmap(base64Text)
                                        if (decoded != null) {
                                            sampleBmp = decoded
                                            val outDir = File(context.cacheDir, "media_studio/base64").apply { mkdirs() }
                                            val outFile = File(outDir, "Dyne_FromBase64_${System.currentTimeMillis()}.png")
                                            FileOutputStream(outFile).use { decoded.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                            val (savedPath, savedUri) = MediaStudioEngine.saveFileToMediaStore(
                                                context, outFile, "image/png", MediaStudioEngine.MediaStoreCategory.IMAGE
                                            )
                                            val res = MediaExportResult(
                                                toolId = "pdf_image_tools",
                                                toolName = "Base64 to Image",
                                                fileName = outFile.name,
                                                mimeType = "image/png",
                                                localCacheFile = outFile,
                                                savedMediaStorePath = savedPath,
                                                savedMediaStoreUri = savedUri,
                                                beforeSizeBytes = base64Text.length.toLong(),
                                                afterSizeBytes = outFile.length(),
                                                beforePreviewBitmap = decoded,
                                                afterPreviewBitmap = decoded,
                                                summaryText = "Decoded Base64 to PNG (${decoded.width}×${decoded.height} px)"
                                            )
                                            exportResult = res
                                        } else {
                                            statusBanner = "Paste a valid Base64 image string first."
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Base64 → Image")
                            }
                        }
                        OutlinedTextField(
                            value = base64Text.take(600),
                            onValueChange = { base64Text = it },
                            label = { Text("Base64 Data URI") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                    }
                }

                statusBanner?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        MediaProcessingProgressCard(
            isProcessing = isProcessing,
            progressCurrent = progressCurrent,
            progressTotal = progressTotal,
            statusText = progressStatus,
            onCancel = {
                activeJob?.cancel()
                isProcessing = false
            }
        )

        MediaExportResultCard(
            result = exportResult,
            onConvertAnother = { exportResult = null },
            onToast = { statusBanner = it }
        )

        MediaStudioRecentFilesSection(
            scanViewModel = scanViewModel,
            onToast = { statusBanner = it }
        )
    }
}
