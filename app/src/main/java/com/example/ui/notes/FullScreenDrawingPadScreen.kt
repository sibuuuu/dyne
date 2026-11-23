package com.example.ui.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.data.NoteRecord
import com.example.data.PdfPageSize
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassSheet
import com.example.ui.components.HazeState
import com.example.ui.components.ProBadge
import com.example.ui.components.pressScaleClickable
import com.example.ui.scan.ScanViewModel
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class DockPopoverPanel {
    NONE,
    PENS,
    COLORS,
    THICKNESS,
    PAPERS
}

/**
 * FULL-SCREEN DRAWING PAD (immersive route, hides the floating tab bar):
 * - Canvas fills the screen on an A4-width vertically scrollable procedural paper page
 * - Slim auto-hiding top bar (Back with auto-save, Title, Undo, Redo, Handwriting-to-Text, Export)
 * - Floating glass tool dock (Pen, Highlighter, Eraser, Lasso, Color, Thickness, Paper)
 * - 6 Pens: Ballpoint, Fountain (pressure sensitive), Marker, Pencil (grainy), Chalk, Highlighter
 * - 10 Procedural Paper surfaces generated in code with grain, fibers, vignette, and edge shadow
 * - Smooth Bezier strokes, stylus pressure & tilt, palm rejection ("Fingers can draw" toggle),
 *   pinch zoom & pan, two-finger tap to undo, 12 colors + custom picker, landscape support,
 *   keep screen on, 60fps committed strokes bitmap layer, hold-to-snap Shape Recognition, and
 *   ML Kit Digital Ink Handwriting-to-Text.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun FullScreenDrawingPadScreen(
    existingNote: NoteRecord?,
    scanViewModel: ScanViewModel,
    isPro: Boolean,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val featureGate = LocalFeatureGate.current

    val isNotesUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.UNLIMITED_NOTES_PENS_PAPERS) == true)

    fun requireNotesPro(
        title: String,
        description: String,
        onAllowed: () -> Unit
    ) {
        if (isNotesUnlocked) {
            onAllowed()
        } else if (featureGate != null) {
            featureGate.require(
                feature = PremiumFeature.UNLIMITED_NOTES_PENS_PAPERS,
                customTitle = title,
                customDescription = description,
                onUnlocked = onAllowed
            )
        } else {
            onOpenPaywall()
        }
    }

    // Keep screen on while the Full-Screen Drawing Pad is active
    DisposableEffect(view) {
        val prev = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = prev
        }
    }

    var noteId by rememberSaveable { mutableLongStateOf(existingNote?.id ?: 0L) }
    var sketchTitle by rememberSaveable {
        mutableStateOf(existingNote?.title ?: "")
    }
    var paperStyleId by rememberSaveable {
        mutableStateOf(existingNote?.paperStyleId?.ifBlank { PaperStyle.PLAIN_WHITE.id } ?: PaperStyle.PLAIN_WHITE.id)
    }
    val selectedPaper = remember(paperStyleId) { PaperStyle.fromId(paperStyleId) }

    var activeDockMode by rememberSaveable { mutableStateOf(DrawingDockMode.PEN) }
    var activePenType by rememberSaveable { mutableStateOf(SketchPenType.BALLPOINT) }
    var selectedColorHex by rememberSaveable {
        mutableStateOf(if (selectedPaper.isDarkSurface) "#FFFFFF" else "#111827")
    }
    var customColorR by rememberSaveable { mutableIntStateOf(56) }
    var customColorG by rememberSaveable { mutableIntStateOf(189) }
    var customColorB by rememberSaveable { mutableIntStateOf(248) }
    var showCustomColorSliders by rememberSaveable { mutableStateOf(false) }

    var strokeWidthDp by rememberSaveable { mutableFloatStateOf(5.5f) }
    var shapeRecognitionEnabled by rememberSaveable { mutableStateOf(true) }

    // Stylus detection & Palm rejection ("Fingers can draw" toggle when stylus is detected)
    var stylusDetected by rememberSaveable { mutableStateOf(false) }
    var fingersCanDraw by rememberSaveable { mutableStateOf(true) }

    // Active popover above the floating glass tool dock
    var openPopover by remember { mutableStateOf(DockPopoverPanel.NONE) }
    var showTitleEditDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var selectedExportPageSize by remember { mutableStateOf(PdfPageSize.defaultForLocale()) }

    // Handwriting recognition state
    var isRecognizingInk by remember { mutableStateOf(false) }
    var recognizedTextDraft by rememberSaveable {
        mutableStateOf(existingNote?.recognizedHandwritingText ?: "")
    }
    var showHandwritingSheet by remember { mutableStateOf(false) }
    var statusBannerText by remember { mutableStateOf<String?>(null) }

    // Auto-hiding top bar while drawing
    var isCurrentlyDrawingStroke by remember { mutableStateOf(false) }
    var chromeManuallyHidden by remember { mutableStateOf(false) }
    val showTopAndBottomChrome = !isCurrentlyDrawingStroke && !chromeManuallyHidden

    // Strokes & undo/redo stacks
    val strokes = remember(existingNote?.id) {
        mutableStateListOf<SketchStroke>().apply {
            if (existingNote != null && existingNote.strokesJson.isNotBlank()) {
                addAll(DrawingPadEngine.deserializeStrokes(existingNote.strokesJson))
            }
        }
    }
    val undoneStrokes = remember { mutableStateListOf<SketchStroke>() }
    val activeStrokePoints = remember { mutableStateListOf<StylusPoint>() }
    var activeStrokeSnappedLabel by remember { mutableStateOf<String?>(null) }

    // Lasso selection state
    val lassoPolygonPoints = remember { mutableStateListOf<Offset>() }
    var selectedStrokeIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var lassoSelectionBounds by remember { mutableStateOf<Rect?>(null) }
    var isDraggingLassoSelection by remember { mutableStateOf(false) }
    var lastLassoDragPoint by remember { mutableStateOf(Offset.Zero) }

    // Virtual A4 page dimensions (scrollable vertically, pinch-zoomable & pannable)
    val pageLogicalWidth = 900
    val pageLogicalHeight = 1520 // Extended A4-width vertically scrollable sheet
    var zoomScale by rememberSaveable { mutableFloatStateOf(1f) }
    var panOffsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var panOffsetY by rememberSaveable { mutableFloatStateOf(0f) }

    // 60fps Committed Layer Bitmap (rebuilt ONLY when committed strokes or paper style change)
    var committedVersion by remember { mutableIntStateOf(0) }
    val committedBitmap = remember(selectedPaper, committedVersion, strokes.size) {
        val bmp = Bitmap.createBitmap(pageLogicalWidth, pageLogicalHeight, Bitmap.Config.ARGB_8888)
        val c = AndroidCanvas(bmp)
        DrawingPadEngine.drawProceduralPaper(c, selectedPaper, pageLogicalWidth, pageLogicalHeight)
        for (s in strokes) {
            DrawingPadEngine.drawStrokeOnAndroidCanvas(c, s, selectedPaper, 1f, 1f)
        }
        bmp
    }

    // Hold-at-end of stroke timer for Premium Shape Recognition
    var lastMoveTimestampMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isCurrentlyDrawingStroke, lastMoveTimestampMs, shapeRecognitionEnabled, isNotesUnlocked) {
        if (
            isCurrentlyDrawingStroke &&
            activeDockMode == DrawingDockMode.PEN &&
            shapeRecognitionEnabled &&
            isNotesUnlocked &&
            activeStrokePoints.size >= 8 &&
            activeStrokeSnappedLabel == null
        ) {
            val capturedMoveTime = lastMoveTimestampMs
            delay(430L)
            if (isCurrentlyDrawingStroke && lastMoveTimestampMs == capturedMoveTime && activeStrokePoints.size >= 8) {
                val snapped = DrawingPadEngine.trySnapRecognizedShape(activeStrokePoints.toList())
                if (snapped != null) {
                    activeStrokePoints.clear()
                    activeStrokePoints.addAll(snapped.second)
                    activeStrokeSnappedLabel = snapped.first
                    FeedbackHelper.confirmClick(context)
                    statusBannerText = "Snapped to ${snapped.first}"
                }
            }
        }
    }

    // Auto-clear transient status banner
    LaunchedEffect(statusBannerText) {
        if (statusBannerText != null) {
            delay(2400L)
            statusBannerText = null
        }
    }

    fun performSaveAndExit(silent: Boolean = false, closeAfter: Boolean = true) {
        if (strokes.isEmpty() && noteId == 0L) {
            if (closeAfter) onClose()
            return
        }
        val renderedBmp = DrawingPadEngine.renderFullSheetToBitmap(
            paper = selectedPaper,
            strokes = strokes.toList(),
            sourceWidth = pageLogicalWidth,
            sourceHeight = pageLogicalHeight,
            targetWidth = 1080
        )
        val strokesJson = DrawingPadEngine.serializeStrokes(strokes.toList())
        scanViewModel.saveDrawingNote(
            context = context,
            title = sketchTitle.ifBlank { "Sketch" },
            paperColorHex = selectedPaper.baseColorHex,
            bitmap = renderedBmp,
            isPinned = existingNote?.isPinned ?: false,
            existingId = noteId,
            paperStyleId = selectedPaper.id,
            strokesJson = strokesJson,
            recognizedHandwritingText = recognizedTextDraft,
            silentAutoSave = silent
        ) { saved ->
            noteId = saved.id
            if (closeAfter) {
                onClose()
            }
        }
    }

    BackHandler {
        performSaveAndExit(silent = false, closeAfter = true)
    }

    // Multi-touch tracking for 2-finger tap undo and 2-finger pinch zoom & pan
    var twoFingerDownTimeMs by remember { mutableLongStateOf(0L) }
    var twoFingerMaxMovePx by remember { mutableFloatStateOf(0f) }
    var prevTwoFingerMid by remember { mutableStateOf<Offset?>(null) }
    var prevTwoFingerSpan by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .testTag("full_screen_drawing_pad")
    ) {
        val viewportW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val viewportH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Fit A4 page width comfortably with subtle side margin
        val horizontalMarginPx = if (isLandscape) viewportW * 0.14f else 28f
        val baseFitScale = ((viewportW - horizontalMarginPx * 2f) / pageLogicalWidth.toFloat()).coerceAtLeast(0.25f)
        val effectiveScale = baseFitScale * zoomScale
        val scaledPageW = pageLogicalWidth * effectiveScale
        val scaledPageH = pageLogicalHeight * effectiveScale

        val minPanY = min(0f, viewportH - scaledPageH - 140f)
        val maxPanY = 110f
        val clampedPanY = panOffsetY.coerceIn(minPanY, maxPanY)
        val maxPanX = max(40f, (scaledPageW - viewportW) * 0.5f + 80f)
        val clampedPanX = panOffsetX.coerceIn(-maxPanX, maxPanX)

        val sheetLeft = (viewportW - scaledPageW) * 0.5f + clampedPanX
        val sheetTop = 88f + clampedPanY

        fun viewToPageCoords(vx: Float, vy: Float): Offset {
            val px = ((vx - sheetLeft) / effectiveScale).coerceIn(0f, pageLogicalWidth.toFloat())
            val py = ((vy - sheetTop) / effectiveScale).coerceIn(0f, pageLogicalHeight.toFloat())
            return Offset(px, py)
        }

        // MAIN INTERACTIVE 60FPS DRAWING CANVAS
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    val pointerCount = event.pointerCount
                    val action = event.actionMasked

                    // Check if any pointer is a hardware stylus
                    var hasStylusInEvent = false
                    for (i in 0 until pointerCount) {
                        val toolType = event.getToolType(i)
                        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
                            hasStylusInEvent = true
                        }
                    }
                    if (hasStylusInEvent && !stylusDetected) {
                        stylusDetected = true
                        fingersCanDraw = false // Enable palm rejection automatically when stylus first touches!
                        statusBannerText = "Stylus detected • Palm rejection active"
                    }

                    // 2-Finger Gesture Handling: Two-Finger Tap to Undo + Pinch Zoom & Pan
                    if (pointerCount >= 2) {
                        // Cancel any accidental single-finger stroke that just started
                        if (activeStrokePoints.size <= 4) {
                            activeStrokePoints.clear()
                            lassoPolygonPoints.clear()
                            isCurrentlyDrawingStroke = false
                        }
                        val x0 = event.getX(0)
                        val y0 = event.getY(0)
                        val x1 = event.getX(1)
                        val y1 = event.getY(1)
                        val mid = Offset((x0 + x1) * 0.5f, (y0 + y1) * 0.5f)
                        val span = hypot(x1 - x0, y1 - y0).coerceAtLeast(10f)

                        if (action == MotionEvent.ACTION_POINTER_DOWN) {
                            twoFingerDownTimeMs = System.currentTimeMillis()
                            twoFingerMaxMovePx = 0f
                            prevTwoFingerMid = mid
                            prevTwoFingerSpan = span
                        } else if (action == MotionEvent.ACTION_MOVE) {
                            val prevMid = prevTwoFingerMid
                            val prevSpan = prevTwoFingerSpan
                            if (prevMid != null && prevSpan > 10f) {
                                val dx = mid.x - prevMid.x
                                val dy = mid.y - prevMid.y
                                val ds = kotlin.math.abs(span - prevSpan)
                                twoFingerMaxMovePx = max(twoFingerMaxMovePx, hypot(dx, dy) + ds)

                                if (twoFingerMaxMovePx > 18f) {
                                    val zoomDelta = (span / prevSpan).coerceIn(0.85f, 1.18f)
                                    zoomScale = (zoomScale * zoomDelta).coerceIn(0.85f, 3.5f)
                                    panOffsetX = (panOffsetX + dx).coerceIn(-maxPanX, maxPanX)
                                    panOffsetY = (panOffsetY + dy).coerceIn(minPanY, maxPanY)
                                }
                            }
                            prevTwoFingerMid = mid
                            prevTwoFingerSpan = span
                        } else if (action == MotionEvent.ACTION_POINTER_UP) {
                            val elapsed = System.currentTimeMillis() - twoFingerDownTimeMs
                            if (elapsed in 15L..260L && twoFingerMaxMovePx < 24f) {
                                // Two-finger tap to undo!
                                if (strokes.isNotEmpty()) {
                                    val popped = strokes.removeAt(strokes.lastIndex)
                                    undoneStrokes.add(popped)
                                    committedVersion++
                                    FeedbackHelper.tick(context)
                                    statusBannerText = "Two-finger Undo"
                                }
                            }
                            prevTwoFingerMid = null
                        }
                        return@pointerInteropFilter true
                    }

                    // Single pointer handling (Stylus or Finger)
                    val toolType = event.getToolType(0)
                    val isStylusOrEraser = toolType == MotionEvent.TOOL_TYPE_STYLUS ||
                        toolType == MotionEvent.TOOL_TYPE_ERASER
                    val isHardwareEraserTip = toolType == MotionEvent.TOOL_TYPE_ERASER

                    // Palm Rejection: if stylus is active and "Fingers can draw" is disabled,
                    // finger touch scrolls the page vertically instead of drawing!
                    if (stylusDetected && !fingersCanDraw && !isStylusOrEraser) {
                        when (action) {
                            MotionEvent.ACTION_DOWN -> {
                                prevTwoFingerMid = Offset(event.x, event.y)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                prevTwoFingerMid?.let { prev ->
                                    val dy = event.y - prev.y
                                    panOffsetY = (panOffsetY + dy).coerceIn(minPanY, maxPanY)
                                }
                                prevTwoFingerMid = Offset(event.x, event.y)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                prevTwoFingerMid = null
                            }
                        }
                        return@pointerInteropFilter true
                    }

                    val pagePt = viewToPageCoords(event.x, event.y)
                    val pressure = event.pressure.coerceIn(0.15f, 1.4f)
                    val tilt = event.getAxisValue(MotionEvent.AXIS_TILT).coerceIn(0f, 1.57f)

                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            openPopover = DockPopoverPanel.NONE
                            if (activeDockMode == DrawingDockMode.LASSO) {
                                val bounds = lassoSelectionBounds
                                if (bounds != null && selectedStrokeIds.isNotEmpty() && bounds.contains(pagePt)) {
                                    isDraggingLassoSelection = true
                                    lastLassoDragPoint = pagePt
                                } else {
                                    isDraggingLassoSelection = false
                                    selectedStrokeIds = emptySet()
                                    lassoSelectionBounds = null
                                    lassoPolygonPoints.clear()
                                    lassoPolygonPoints.add(pagePt)
                                }
                            } else {
                                isCurrentlyDrawingStroke = true
                                activeStrokeSnappedLabel = null
                                activeStrokePoints.clear()
                                activeStrokePoints.add(
                                    StylusPoint(
                                        x = pagePt.x,
                                        y = pagePt.y,
                                        pressure = pressure,
                                        tilt = tilt,
                                        timestampMs = System.currentTimeMillis()
                                    )
                                )
                                lastMoveTimestampMs = System.currentTimeMillis()
                            }
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (activeDockMode == DrawingDockMode.LASSO) {
                                if (isDraggingLassoSelection && selectedStrokeIds.isNotEmpty()) {
                                    val dx = pagePt.x - lastLassoDragPoint.x
                                    val dy = pagePt.y - lastLassoDragPoint.y
                                    if (hypot(dx, dy) > 1f) {
                                        for (idx in strokes.indices) {
                                            val st = strokes[idx]
                                            if (st.id in selectedStrokeIds) {
                                                strokes[idx] = st.copy(
                                                    points = st.points.map { p ->
                                                        p.copy(
                                                            x = (p.x + dx).coerceIn(0f, pageLogicalWidth.toFloat()),
                                                            y = (p.y + dy).coerceIn(0f, pageLogicalHeight.toFloat())
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        lassoSelectionBounds = lassoSelectionBounds?.translate(dx, dy)
                                        lastLassoDragPoint = pagePt
                                        committedVersion++
                                    }
                                } else {
                                    lassoPolygonPoints.add(pagePt)
                                }
                            } else if (isCurrentlyDrawingStroke) {
                                val lastPt = activeStrokePoints.lastOrNull()
                                val movedDist = if (lastPt != null) hypot(pagePt.x - lastPt.x, pagePt.y - lastPt.y) else 99f
                                if (movedDist >= 2.2f) {
                                    if (activeStrokeSnappedLabel != null && movedDist > 18f) {
                                        // User resumed freehand movement after snap
                                        activeStrokeSnappedLabel = null
                                    }
                                    if (activeStrokeSnappedLabel == null) {
                                        activeStrokePoints.add(
                                            StylusPoint(
                                                x = pagePt.x,
                                                y = pagePt.y,
                                                pressure = pressure,
                                                tilt = tilt,
                                                timestampMs = System.currentTimeMillis()
                                            )
                                        )
                                        lastMoveTimestampMs = System.currentTimeMillis()
                                    }
                                }
                            }
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (activeDockMode == DrawingDockMode.LASSO) {
                                if (isDraggingLassoSelection) {
                                    isDraggingLassoSelection = false
                                } else if (lassoPolygonPoints.size >= 3) {
                                    val minX = lassoPolygonPoints.minOf { it.x }
                                    val maxX = lassoPolygonPoints.maxOf { it.x }
                                    val minY = lassoPolygonPoints.minOf { it.y }
                                    val maxY = lassoPolygonPoints.maxOf { it.y }
                                    val rect = Rect(minX, minY, maxX, maxY)
                                    val matched = strokes.filter { st ->
                                        st.points.any { p -> rect.contains(Offset(p.x, p.y)) }
                                    }.map { it.id }.toSet()
                                    selectedStrokeIds = matched
                                    lassoSelectionBounds = if (matched.isNotEmpty()) rect else null
                                    lassoPolygonPoints.clear()
                                    if (matched.isNotEmpty()) {
                                        statusBannerText = "${matched.size} stroke(s) selected • Drag to move"
                                    }
                                } else {
                                    lassoPolygonPoints.clear()
                                }
                            } else if (isCurrentlyDrawingStroke) {
                                isCurrentlyDrawingStroke = false
                                if (activeStrokePoints.isNotEmpty()) {
                                    val resolvedPen = if (activeDockMode == DrawingDockMode.HIGHLIGHTER) {
                                        SketchPenType.HIGHLIGHTER
                                    } else {
                                        activePenType
                                    }
                                    val isErase = activeDockMode == DrawingDockMode.ERASER || isHardwareEraserTip
                                    strokes.add(
                                        SketchStroke(
                                            points = activeStrokePoints.toList(),
                                            colorHex = selectedColorHex,
                                            penType = resolvedPen,
                                            isEraser = isErase,
                                            strokeWidthPx = strokeWidthDp * 2.1f,
                                            snappedShapeName = activeStrokeSnappedLabel
                                        )
                                    )
                                    undoneStrokes.clear()
                                    activeStrokePoints.clear()
                                    activeStrokeSnappedLabel = null
                                    committedVersion++
                                }
                            }
                        }
                    }
                    true
                }
        ) {
            // 1. Physical Paper Drop Shadow behind the A4 sheet
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(sheetLeft + 6f, sheetTop + 12f),
                size = androidx.compose.ui.geometry.Size(scaledPageW, scaledPageH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
            )

            // 2. Draw the 60fps Committed Layer Bitmap (Procedural Paper + Committed Strokes)
            drawImage(
                image = committedBitmap.asImageBitmap(),
                dstOffset = IntOffset(sheetLeft.roundToInt(), sheetTop.roundToInt()),
                dstSize = IntSize(scaledPageW.roundToInt(), scaledPageH.roundToInt())
            )

            // 3. Draw the single in-progress active stroke directly on the native canvas
            if (activeStrokePoints.isNotEmpty()) {
                val resolvedPen = if (activeDockMode == DrawingDockMode.HIGHLIGHTER) {
                    SketchPenType.HIGHLIGHTER
                } else {
                    activePenType
                }
                val liveStroke = SketchStroke(
                    points = activeStrokePoints,
                    colorHex = selectedColorHex,
                    penType = resolvedPen,
                    isEraser = activeDockMode == DrawingDockMode.ERASER,
                    strokeWidthPx = strokeWidthDp * 2.1f
                )
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    translate(sheetLeft, sheetTop)
                    DrawingPadEngine.drawStrokeOnAndroidCanvas(
                        canvas = this,
                        stroke = liveStroke,
                        paper = selectedPaper,
                        scaleX = effectiveScale,
                        scaleY = effectiveScale
                    )
                    restore()
                }
            }

            // 4. Draw Lasso selection loop or bounding box
            if (lassoPolygonPoints.size >= 2) {
                val path = Path().apply {
                    val first = lassoPolygonPoints.first()
                    moveTo(sheetLeft + first.x * effectiveScale, sheetTop + first.y * effectiveScale)
                    for (i in 1 until lassoPolygonPoints.size) {
                        val pt = lassoPolygonPoints[i]
                        lineTo(sheetLeft + pt.x * effectiveScale, sheetTop + pt.y * effectiveScale)
                    }
                }
                drawPath(
                    path = path,
                    color = theme.accent,
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
            }
            lassoSelectionBounds?.let { b ->
                drawRect(
                    color = theme.accent,
                    topLeft = Offset(sheetLeft + b.left * effectiveScale, sheetTop + b.top * effectiveScale),
                    size = androidx.compose.ui.geometry.Size(b.width * effectiveScale, b.height * effectiveScale),
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                )
            }
        }

        // VERTICAL PAGE SCROLL SLIDER ON RIGHT EDGE (so long A4 pages are effortlessly scrollable with 1 thumb)
        val scrollFraction = if (minPanY < -1f) {
            ((maxPanY - clampedPanY) / (maxPanY - minPanY)).coerceIn(0f, 1f)
        } else 0f
        if (minPanY < -20f && showTopAndBottomChrome) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC0F172A))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 6.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Top",
                    style = type.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (scrollFraction < 0.2f) theme.accent else theme.textSecondary,
                    modifier = Modifier.pressScaleClickable {
                        panOffsetY = maxPanY
                    }
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(90.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .padding(top = (scrollFraction * 64f).dp)
                            .clip(CircleShape)
                            .background(theme.accent)
                    )
                }
                Text(
                    text = "End",
                    style = type.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (scrollFraction > 0.8f) theme.accent else theme.textSecondary,
                    modifier = Modifier.pressScaleClickable {
                        panOffsetY = minPanY
                    }
                )
            }
        }

        // SLIM AUTO-HIDING TOP BAR (Back with auto-save, Title, Undo, Redo, Handwriting-to-Text, Export)
        AnimatedVisibility(
            visible = showTopAndBottomChrome,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(14.dp, RoundedCornerShape(25.dp))
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color(0xE6111827))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(25.dp))
                        .padding(horizontal = 10.dp)
                ) {
                    // Back with auto-save
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .pressScaleClickable(
                                testTag = "drawing_pad_back_button",
                                onClick = {
                                    FeedbackHelper.tick(context)
                                    performSaveAndExit(silent = false, closeAfter = true)
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back and auto-save",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Title (tap to rename)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .pressScaleClickable { showTitleEditDialog = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = sketchTitle.ifBlank { "Untitled Sketch" },
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${selectedPaper.label} • Auto-save",
                                style = type.caption.copy(fontSize = 10.sp),
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename sketch",
                            tint = theme.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Undo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .pressScaleClickable(
                                testTag = "drawing_pad_undo",
                                onClick = {
                                    if (strokes.isNotEmpty()) {
                                        val popped = strokes.removeAt(strokes.lastIndex)
                                        undoneStrokes.add(popped)
                                        committedVersion++
                                        FeedbackHelper.tick(context)
                                    }
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (strokes.isNotEmpty()) theme.accent else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Redo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .pressScaleClickable(
                                testTag = "drawing_pad_redo",
                                onClick = {
                                    if (undoneStrokes.isNotEmpty()) {
                                        val restored = undoneStrokes.removeAt(undoneStrokes.lastIndex)
                                        strokes.add(restored)
                                        committedVersion++
                                        FeedbackHelper.tick(context)
                                    }
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (undoneStrokes.isNotEmpty()) theme.accent else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Handwriting to Text (ML Kit Digital Ink Recognition - Premium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                if (isNotesUnlocked) theme.accent.copy(alpha = 0.5f) else ProGold.copy(alpha = 0.65f),
                                RoundedCornerShape(17.dp)
                            )
                            .pressScaleClickable(
                                testTag = "drawing_pad_handwriting_ocr",
                                onClick = {
                                    requireNotesPro(
                                        title = "Handwriting to Text",
                                        description = "Convert handwritten ink strokes into editable text on your device."
                                    ) {
                                        showHandwritingSheet = true
                                        isRecognizingInk = true
                                        DrawingPadEngine.recognizeHandwritingWithMlKit(
                                            strokes = strokes.toList(),
                                            onStatus = { msg -> statusBannerText = msg },
                                            onResult = { recognized, msg ->
                                                isRecognizingInk = false
                                                if (!recognized.isNullOrBlank()) {
                                                    recognizedTextDraft = recognized
                                                }
                                                statusBannerText = msg
                                            }
                                        )
                                    }
                                }
                            )
                            .padding(horizontal = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "Handwriting to text",
                            tint = if (isNotesUnlocked) theme.accent else ProGold,
                            modifier = Modifier.size(15.dp)
                        )
                        if (!isNotesUnlocked) {
                            ProBadge()
                        }
                    }

                    // Export Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(theme.accent)
                            .pressScaleClickable(
                                testTag = "drawing_pad_export_button",
                                onClick = {
                                    FeedbackHelper.tick(context)
                                    showExportSheet = true
                                }
                            )
                            .padding(horizontal = 11.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Export sketch",
                            tint = BentoTextBlack,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Export",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = BentoTextBlack
                        )
                    }
                }

                // Secondary Sub-Bar: Stylus Palm Rejection ("Fingers can draw" toggle), Zoom Reset, & Stylus simulator toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Stylus Mode / "Fingers can draw" toggle (always accessible or auto-shown when stylus detected)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xD9111827))
                            .border(
                                1.dp,
                                if (stylusDetected && !fingersCanDraw) theme.accent else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(14.dp)
                            )
                            .pressScaleClickable {
                                stylusDetected = true
                                fingersCanDraw = !fingersCanDraw
                                statusBannerText = if (fingersCanDraw) {
                                    "Fingers can draw: ON"
                                } else {
                                    "Palm rejection: Stylus draws, fingers scroll"
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("fingers_can_draw_toggle")
                    ) {
                        Icon(
                            imageVector = if (fingersCanDraw) Icons.Default.TouchApp else Icons.Default.PanTool,
                            contentDescription = "Fingers can draw toggle",
                            tint = if (fingersCanDraw) theme.accent else theme.bentoSky,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (fingersCanDraw) "Fingers can draw: ON" else "Fingers can draw: OFF (Palm Reject)",
                            style = type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }

                    // Zoom Reset pill if zoomed or panned
                    if (kotlin.math.abs(zoomScale - 1f) > 0.04f || kotlin.math.abs(panOffsetX) > 12f) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xD9111827))
                                .border(1.dp, theme.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .pressScaleClickable {
                                    zoomScale = 1f
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${(zoomScale * 100).roundToInt()}% • Reset View",
                                style = type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = theme.accent
                            )
                        }
                    }

                    // Hide UI pill for distraction-free full canvas view
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xD9111827))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                            .pressScaleClickable { chromeManuallyHidden = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Auto-hide bars",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Hide Bars",
                            style = type.caption.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Small floating pill to restore top/bottom bars when manually hidden
        if (chromeManuallyHidden && !isCurrentlyDrawingStroke) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xD9111827))
                    .border(1.dp, theme.accent.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .pressScaleClickable { chromeManuallyHidden = false }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Show Tools",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
            }
        }

        // Transient status toast banner (Shape snapped, 2-finger undo, Handwriting status)
        AnimatedVisibility(
            visible = statusBannerText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 104.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xEE0F172A))
                    .border(1.dp, theme.accent.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = statusBannerText.orEmpty(),
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
            }
        }

        // FLOATING GLASS TOOL DOCK + POPOVER PANELS (Pen, Highlighter, Eraser, Lasso, Color, Thickness, Paper)
        AnimatedVisibility(
            visible = showTopAndBottomChrome,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.widthIn(max = 640.dp)
            ) {
                // POPOVER PANEL ABOVE DOCK
                when (openPopover) {
                    DockPopoverPanel.PENS -> {
                        GlassPopoverCard(
                            title = "6 PRECISION PENS",
                            onClose = { openPopover = DockPopoverPanel.NONE }
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SketchPenType.entries.forEach { pen ->
                                    val isSelected = activeDockMode == DrawingDockMode.PEN && activePenType == pen
                                    val isLockedPen = !pen.isFree && !isNotesUnlocked
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) theme.accent.copy(alpha = 0.22f)
                                                else Color.White.copy(alpha = 0.06f)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = when {
                                                    isSelected -> theme.accent
                                                    isLockedPen -> ProGold.copy(alpha = 0.55f)
                                                    else -> Color.White.copy(alpha = 0.12f)
                                                },
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .pressScaleClickable(
                                                testTag = "pen_option_${pen.id.lowercase()}",
                                                onClick = {
                                                    FeedbackHelper.tick(context)
                                                    if (isLockedPen) {
                                                        requireNotesPro(
                                                            title = "${pen.label} Pen",
                                                            description = "Unlock all 6 pens (Ballpoint, Fountain, Marker, Pencil, Chalk, Highlighter) and all 10 procedural papers."
                                                        ) {
                                                            activeDockMode = if (pen == SketchPenType.HIGHLIGHTER) {
                                                                DrawingDockMode.HIGHLIGHTER
                                                            } else {
                                                                DrawingDockMode.PEN
                                                            }
                                                            activePenType = pen
                                                            strokeWidthDp = pen.defaultWidthDp
                                                        }
                                                    } else {
                                                        activeDockMode = if (pen == SketchPenType.HIGHLIGHTER) {
                                                            DrawingDockMode.HIGHLIGHTER
                                                        } else {
                                                            DrawingDockMode.PEN
                                                        }
                                                        activePenType = pen
                                                        strokeWidthDp = pen.defaultWidthDp
                                                    }
                                                }
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(
                                                    text = pen.label,
                                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSelected) theme.accent else Color.White
                                                )
                                                if (isLockedPen) {
                                                    ProBadge()
                                                }
                                            }
                                            Text(
                                                text = pen.subtitle,
                                                style = type.caption.copy(fontSize = 10.sp),
                                                color = Color.White.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DockPopoverPanel.COLORS -> {
                        GlassPopoverCard(
                            title = "12 INK COLORS + CUSTOM PICKER",
                            onClose = { openPopover = DockPopoverPanel.NONE }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                DrawingInkColors12.forEach { option ->
                                    val selected = selectedColorHex.equals(option.hex, ignoreCase = true)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(option.color)
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) theme.accent else Color.White.copy(alpha = 0.35f),
                                                shape = CircleShape
                                            )
                                            .pressScaleClickable {
                                                FeedbackHelper.tick(context)
                                                selectedColorHex = option.hex
                                            }
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = option.name,
                                                tint = if (option.hex == "#FFFFFF" || option.hex == "#C6F27A") BentoTextBlack else Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                // Custom Picker Toggle Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (showCustomColorSliders) theme.accent else Color.White.copy(alpha = 0.1f))
                                        .pressScaleClickable {
                                            showCustomColorSliders = !showCustomColorSliders
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ColorLens,
                                        contentDescription = "Custom color",
                                        tint = if (showCustomColorSliders) BentoTextBlack else theme.accent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Custom",
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = if (showCustomColorSliders) BentoTextBlack else Color.White
                                    )
                                }
                            }

                            if (showCustomColorSliders) {
                                val customHex = String.format("#%02X%02X%02X", customColorR, customColorG, customColorB)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(customColorR, customColorG, customColorB))
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                            Text(
                                                text = "Custom Hex $customHex",
                                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                        }
                                        GlassButton(
                                            text = "Use Color",
                                            accentFill = theme.accent,
                                            onClick = {
                                                selectedColorHex = customHex
                                            }
                                        )
                                    }
                                    Slider(
                                        value = customColorR.toFloat(),
                                        onValueChange = {
                                            customColorR = it.roundToInt().coerceIn(0, 255)
                                            selectedColorHex = String.format("#%02X%02X%02X", customColorR, customColorG, customColorB)
                                        },
                                        valueRange = 0f..255f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
                                    )
                                    Slider(
                                        value = customColorG.toFloat(),
                                        onValueChange = {
                                            customColorG = it.roundToInt().coerceIn(0, 255)
                                            selectedColorHex = String.format("#%02X%02X%02X", customColorR, customColorG, customColorB)
                                        },
                                        valueRange = 0f..255f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                                    )
                                    Slider(
                                        value = customColorB.toFloat(),
                                        onValueChange = {
                                            customColorB = it.roundToInt().coerceIn(0, 255)
                                            selectedColorHex = String.format("#%02X%02X%02X", customColorR, customColorG, customColorB)
                                        },
                                        valueRange = 0f..255f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                                    )
                                }
                            }
                        }
                    }

                    DockPopoverPanel.THICKNESS -> {
                        GlassPopoverCard(
                            title = "STROKE THICKNESS & SHAPE SNAP",
                            onClose = { openPopover = DockPopoverPanel.NONE }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${strokeWidthDp.roundToInt()} pt",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = theme.accent,
                                    modifier = Modifier.width(44.dp)
                                )
                                Slider(
                                    value = strokeWidthDp,
                                    onValueChange = { strokeWidthDp = it },
                                    valueRange = 1.5f..28f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = theme.accent,
                                        activeTrackColor = theme.accent
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Hold-to-Snap Shape Recognition toggle (Premium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Shape Recognition (Hold at end)",
                                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        if (!isNotesUnlocked) {
                                            ProBadge()
                                        }
                                    }
                                    Text(
                                        text = "Pause briefly at stroke end to snap lines, circles & rectangles",
                                        style = type.caption.copy(fontSize = 10.sp),
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                                Switch(
                                    checked = shapeRecognitionEnabled && isNotesUnlocked,
                                    onCheckedChange = { checked ->
                                        if (!isNotesUnlocked) {
                                            requireNotesPro(
                                                title = "Shape Recognition",
                                                description = "Hold at the end of a stroke to automatically snap lines, circles, and rectangles."
                                            ) {
                                                shapeRecognitionEnabled = true
                                            }
                                        } else {
                                            shapeRecognitionEnabled = checked
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = BentoTextBlack, checkedTrackColor = theme.accent)
                                )
                            }
                        }
                    }

                    DockPopoverPanel.PAPERS -> {
                        GlassPopoverCard(
                            title = "10 PROCEDURAL PAPER SURFACES",
                            onClose = { openPopover = DockPopoverPanel.NONE }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                PaperStyle.entries.forEach { paper ->
                                    val isSelected = selectedPaper == paper
                                    val isLockedPaper = !paper.isFree && !isNotesUnlocked
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier
                                            .width(86.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) theme.accent.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = when {
                                                    isSelected -> theme.accent
                                                    isLockedPaper -> ProGold.copy(alpha = 0.6f)
                                                    else -> Color.White.copy(alpha = 0.14f)
                                                },
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .pressScaleClickable(
                                                testTag = "paper_style_${paper.id.lowercase()}",
                                                onClick = {
                                                    FeedbackHelper.tick(context)
                                                    if (isLockedPaper) {
                                                        requireNotesPro(
                                                            title = "${paper.label} Paper",
                                                            description = "Unlock all 10 procedural paper surfaces (Warm Cream, Grid, Dotted, Graph, Blueprint, Kraft, Blackboard) and all 6 pens."
                                                        ) {
                                                            paperStyleId = paper.id
                                                        }
                                                    } else {
                                                        paperStyleId = paper.id
                                                    }
                                                }
                                            )
                                            .padding(8.dp)
                                    ) {
                                        // Mini procedural swatch
                                        Box(
                                            contentAlignment = Alignment.TopEnd,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(paper.baseComposeColor)
                                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        ) {
                                            if (isLockedPaper) {
                                                Box(modifier = Modifier.padding(3.dp)) {
                                                    ProBadge()
                                                }
                                            }
                                        }
                                        Text(
                                            text = paper.label,
                                            style = type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = if (isSelected) theme.accent else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DockPopoverPanel.NONE -> Unit
                }

                // FLOATING GLASS TOOL DOCK (Pen, Highlighter, Eraser, Lasso, Color, Thickness, Paper)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .shadow(18.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xEB111827))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("drawing_pad_floating_dock")
                ) {
                    // 1. Pen
                    DockToolItem(
                        label = activePenType.label,
                        icon = Icons.Default.Create,
                        selected = activeDockMode == DrawingDockMode.PEN,
                        badgeDotColor = parseHexColor(selectedColorHex, theme.accent),
                        onClick = {
                            FeedbackHelper.tick(context)
                            activeDockMode = DrawingDockMode.PEN
                            openPopover = if (openPopover == DockPopoverPanel.PENS) DockPopoverPanel.NONE else DockPopoverPanel.PENS
                        },
                        testTag = "dock_tool_pen"
                    )

                    // 2. Highlighter
                    DockToolItem(
                        label = "Highlighter",
                        icon = Icons.Default.Brush,
                        selected = activeDockMode == DrawingDockMode.HIGHLIGHTER,
                        onClick = {
                            FeedbackHelper.tick(context)
                            activeDockMode = DrawingDockMode.HIGHLIGHTER
                            openPopover = DockPopoverPanel.NONE
                        },
                        testTag = "dock_tool_highlighter"
                    )

                    // 3. Eraser
                    DockToolItem(
                        label = "Eraser",
                        icon = Icons.Default.Clear,
                        selected = activeDockMode == DrawingDockMode.ERASER,
                        onClick = {
                            FeedbackHelper.tick(context)
                            activeDockMode = DrawingDockMode.ERASER
                            openPopover = DockPopoverPanel.NONE
                        },
                        testTag = "dock_tool_eraser"
                    )

                    // 4. Lasso
                    DockToolItem(
                        label = "Lasso",
                        icon = Icons.Default.Gesture,
                        selected = activeDockMode == DrawingDockMode.LASSO,
                        onClick = {
                            FeedbackHelper.tick(context)
                            activeDockMode = DrawingDockMode.LASSO
                            openPopover = DockPopoverPanel.NONE
                        },
                        testTag = "dock_tool_lasso"
                    )

                    // 5. Color
                    DockToolItem(
                        label = "Color",
                        icon = Icons.Default.ColorLens,
                        selected = openPopover == DockPopoverPanel.COLORS,
                        badgeDotColor = parseHexColor(selectedColorHex, theme.accent),
                        onClick = {
                            FeedbackHelper.tick(context)
                            openPopover = if (openPopover == DockPopoverPanel.COLORS) DockPopoverPanel.NONE else DockPopoverPanel.COLORS
                        },
                        testTag = "dock_tool_color"
                    )

                    // 6. Thickness
                    DockToolItem(
                        label = "${strokeWidthDp.roundToInt()}pt",
                        icon = Icons.Default.LineWeight,
                        selected = openPopover == DockPopoverPanel.THICKNESS,
                        onClick = {
                            FeedbackHelper.tick(context)
                            openPopover = if (openPopover == DockPopoverPanel.THICKNESS) DockPopoverPanel.NONE else DockPopoverPanel.THICKNESS
                        },
                        testTag = "dock_tool_thickness"
                    )

                    // 7. Paper
                    DockToolItem(
                        label = "Paper",
                        icon = Icons.Default.GridOn,
                        selected = openPopover == DockPopoverPanel.PAPERS,
                        onClick = {
                            FeedbackHelper.tick(context)
                            openPopover = if (openPopover == DockPopoverPanel.PAPERS) DockPopoverPanel.NONE else DockPopoverPanel.PAPERS
                        },
                        testTag = "dock_tool_paper"
                    )
                }
            }
        }
    }

    // Rename Sketch Sheet
    if (showTitleEditDialog) {
        GlassSheet(
            onDismissRequest = { showTitleEditDialog = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SKETCH TITLE",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
                OutlinedTextField(
                    value = sketchTitle,
                    onValueChange = { sketchTitle = it },
                    label = { Text("Sketch Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                GlassButton(
                    text = "Done",
                    icon = Icons.Default.Check,
                    accentFill = theme.accent,
                    onClick = { showTitleEditDialog = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Handwriting to Text (ML Kit Digital Ink Recognition) Result Sheet
    if (showHandwritingSheet) {
        GlassSheet(
            onDismissRequest = { showHandwritingSheet = false },
            hazeState = hazeState
        ) {
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
                        text = "HANDWRITING TO TEXT (ML KIT)",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = theme.textSecondary,
                        modifier = Modifier.pressScaleClickable { showHandwritingSheet = false }
                    )
                }

                if (isRecognizingInk) {
                    Text(
                        text = "Recognizing handwriting...",
                        style = type.body,
                        color = theme.textSecondary
                    )
                }

                OutlinedTextField(
                    value = recognizedTextDraft,
                    onValueChange = { recognizedTextDraft = it },
                    placeholder = {
                        Text("Recognized handwriting text will appear here (or edit manually)...")
                    },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Copy Text",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            if (recognizedTextDraft.isNotBlank()) {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Handwriting", recognizedTextDraft))
                                scanViewModel.showToast("Copied recognized text")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Save as Text Note",
                        icon = Icons.Default.Description,
                        accentFill = theme.accent,
                        onClick = {
                            if (recognizedTextDraft.isNotBlank()) {
                                scanViewModel.saveNote(
                                    title = sketchTitle.ifBlank { "Handwriting Note" },
                                    content = recognizedTextDraft,
                                    categoryTag = "Notes",
                                    colorHex = "#38BDF8"
                                )
                                showHandwritingSheet = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Export Sketch Sheet: Share as Image (Free), Save PNG (Free), Export to PDF with Page Size Selector (Pro)
    if (showExportSheet) {
        GlassSheet(
            onDismissRequest = { showExportSheet = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "EXPORT SKETCH",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = theme.textSecondary,
                        modifier = Modifier.pressScaleClickable { showExportSheet = false }
                    )
                }

                // Free: Share as Image (PNG) & Save to Gallery
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Share as Image",
                        icon = Icons.Default.Share,
                        accentFill = theme.accent,
                        onClick = {
                            val bmp = DrawingPadEngine.renderFullSheetToBitmap(
                                paper = selectedPaper,
                                strokes = strokes.toList(),
                                sourceWidth = pageLogicalWidth,
                                sourceHeight = pageLogicalHeight,
                                targetWidth = 1080
                            )
                            scanViewModel.saveDrawingNote(
                                context = context,
                                title = sketchTitle.ifBlank { "Sketch" },
                                paperColorHex = selectedPaper.baseColorHex,
                                bitmap = bmp,
                                existingId = noteId,
                                paperStyleId = selectedPaper.id,
                                strokesJson = DrawingPadEngine.serializeStrokes(strokes.toList()),
                                recognizedHandwritingText = recognizedTextDraft,
                                silentAutoSave = true
                            ) { saved ->
                                noteId = saved.id
                                showExportSheet = false
                                scanViewModel.shareDrawingPng(context, saved) { shareIntent ->
                                    context.startActivity(shareIntent)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "export_sketch_share_image"
                    )

                    GlassButton(
                        text = "Save PNG",
                        icon = Icons.Default.Download,
                        onClick = {
                            val bmp = DrawingPadEngine.renderFullSheetToBitmap(
                                paper = selectedPaper,
                                strokes = strokes.toList(),
                                sourceWidth = pageLogicalWidth,
                                sourceHeight = pageLogicalHeight,
                                targetWidth = 1080
                            )
                            scanViewModel.saveDrawingNote(
                                context = context,
                                title = sketchTitle.ifBlank { "Sketch" },
                                paperColorHex = selectedPaper.baseColorHex,
                                bitmap = bmp,
                                existingId = noteId,
                                paperStyleId = selectedPaper.id,
                                strokesJson = DrawingPadEngine.serializeStrokes(strokes.toList()),
                                recognizedHandwritingText = recognizedTextDraft,
                                silentAutoSave = true
                            ) { saved ->
                                noteId = saved.id
                                showExportSheet = false
                                scanViewModel.exportDrawingPngToPhone(context, saved)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Premium: Export to PDF with Page Size Selector
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PDF PAGE SIZE SELECTOR",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textSecondary
                        )
                        if (!isNotesUnlocked) {
                            ProBadge()
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        PdfPageSize.entries.forEach { sizeOpt ->
                            val selected = selectedExportPageSize == sizeOpt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) theme.accent.copy(alpha = 0.2f) else theme.elevated)
                                    .border(
                                        1.dp,
                                        if (selected) theme.accent else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .pressScaleClickable { selectedExportPageSize = sizeOpt }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sizeOpt.label,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) theme.accent else theme.textPrimary
                                )
                            }
                        }
                    }

                    GlassButton(
                        text = if (isNotesUnlocked) {
                            "Export Sketch to PDF (${selectedExportPageSize.label})"
                        } else {
                            "Export Sketch to PDF (${selectedExportPageSize.label}) • PRO"
                        },
                        icon = Icons.Default.PictureAsPdf,
                        accentFill = if (isNotesUnlocked) theme.accent else ProGold,
                        onClick = {
                            requireNotesPro(
                                title = "Export to PDF with Page Size Selector",
                                description = "Export notes and sketches to standard A4, Letter, Legal, A5, ID Card, or Receipt PDFs."
                            ) {
                                val bmp = DrawingPadEngine.renderFullSheetToBitmap(
                                    paper = selectedPaper,
                                    strokes = strokes.toList(),
                                    sourceWidth = pageLogicalWidth,
                                    sourceHeight = pageLogicalHeight,
                                    targetWidth = 1080
                                )
                                scanViewModel.saveDrawingNote(
                                    context = context,
                                    title = sketchTitle.ifBlank { "Sketch" },
                                    paperColorHex = selectedPaper.baseColorHex,
                                    bitmap = bmp,
                                    existingId = noteId,
                                    paperStyleId = selectedPaper.id,
                                    strokesJson = DrawingPadEngine.serializeStrokes(strokes.toList()),
                                    recognizedHandwritingText = recognizedTextDraft,
                                    silentAutoSave = true
                                ) { saved ->
                                    noteId = saved.id
                                    showExportSheet = false
                                    scanViewModel.exportNotePdf(context, saved, selectedExportPageSize) { chooser ->
                                        context.startActivity(chooser)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "export_sketch_pdf_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun DockToolItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badgeDotColor: Color? = null,
    onClick: () -> Unit,
    testTag: String
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) theme.accent else Color.White.copy(alpha = 0.05f))
            .pressScaleClickable(testTag = testTag, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) BentoTextBlack else Color.White,
                modifier = Modifier.size(18.dp)
            )
            if (badgeDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeDotColor)
                        .border(1.dp, Color.Black, CircleShape)
                )
            }
        }
        Text(
            text = label,
            style = type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = if (selected) BentoTextBlack else Color.White.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}

@Composable
private fun GlassPopoverCard(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF2111827))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.accent
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close panel",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(18.dp)
                    .pressScaleClickable(onClick = onClose)
            )
        }
        content()
    }
}
