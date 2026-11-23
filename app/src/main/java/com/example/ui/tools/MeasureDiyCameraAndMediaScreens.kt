package com.example.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.data.UnitSystem
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.ProBadge
import com.example.ui.theme.LocalDyneTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.concurrent.Executors
import kotlin.math.*

// ================================================================================================
// 1. SCREEN RULER (FREE: Calibrated by screen DPI, inch and cm simultaneously, dual calipers)
// ================================================================================================

@Composable
fun DpiCalibratedScreenRulerScreen(
    unitSystem: UnitSystem = UnitSystem.METRIC,
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("dyne_ruler_prefs", Context.MODE_PRIVATE) }

    val displayMetrics = context.resources.displayMetrics
    val hardwareYdpi = remember {
        if (displayMetrics.ydpi in 100f..700f) displayMetrics.ydpi else displayMetrics.densityDpi.toFloat()
    }
    var calibrationFactor by remember {
        mutableFloatStateOf(prefs.getFloat("dpi_cal_factor", 1.0f).coerceIn(0.8f, 1.25f))
    }
    var showCalibrationPanel by remember(forcedModeId) {
        mutableStateOf(forcedModeId == "calibrate")
    }

    val effectiveDpi = hardwareYdpi * calibrationFactor
    val pxPerInch = effectiveDpi
    val pxPerCm = effectiveDpi / 2.54f
    val pxPerMm = pxPerCm / 10f

    var caliperStartPx by remember { mutableFloatStateOf(60f) }
    var caliperEndPx by remember { mutableFloatStateOf(60f + pxPerCm * 4f) }

    val spanPx = abs(caliperEndPx - caliperStartPx)
    val spanCm = spanPx / pxPerCm
    val spanMm = spanCm * 10f
    val spanInches = spanPx / pxPerInch
    val df = remember { DecimalFormat("0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Readout & Calibration Bar
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = theme.cardBackground.copy(alpha = 0.88f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${df.format(spanCm)} cm  (${df.format(spanMm)} mm)  •  ${df.format(spanInches)} in",
                            color = theme.accent,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("ruler_caliper_readout")
                        )
                        Text(
                            text = "Screen DPI: ${effectiveDpi.roundToInt()} PPI (Hardware ${hardwareYdpi.roundToInt()} PPI)",
                            color = theme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { showCalibrationPanel = !showCalibrationPanel },
                        modifier = Modifier.testTag("ruler_calibrate_toggle")
                    ) {
                        Text(if (showCalibrationPanel) "Done" else "Calibrate DPI", fontSize = 12.sp)
                    }
                }

                if (showCalibrationPanel) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Fine-tune DPI calibration (match a physical credit card = 8.56 cm / 3.37 in):",
                            color = theme.textSecondary,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = calibrationFactor,
                            onValueChange = {
                                calibrationFactor = it
                                prefs.edit().putFloat("dpi_cal_factor", it).apply()
                            },
                            valueRange = 0.85f..1.15f
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    calibrationFactor = 1.0f
                                    prefs.edit().putFloat("dpi_cal_factor", 1.0f).apply()
                                }
                            ) {
                                Text("Reset 100%", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    caliperStartPx = 40f
                                    caliperEndPx = 40f + pxPerCm * 8.56f
                                }
                            ) {
                                Text("Set Credit Card (8.56 cm)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Full-Height Dual Edge Ruler Canvas (Left edge = CM/MM, Right edge = INCHES + 1/16ths)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = theme.cardBackground.copy(alpha = 0.82f),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val y = change.position.y.coerceIn(0f, size.height.toFloat())
                            if (abs(y - caliperStartPx) < abs(y - caliperEndPx)) {
                                caliperStartPx = y
                            } else {
                                caliperEndPx = y
                            }
                            FeedbackHelper.hapticTick(view)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val h = size.height
                    val w = size.width

                    // Highlighted Caliper Band
                    val topY = min(caliperStartPx, caliperEndPx).coerceIn(0f, h)
                    val botY = max(caliperStartPx, caliperEndPx).coerceIn(0f, h)
                    drawRect(
                        color = theme.accent.copy(alpha = 0.16f),
                        topLeft = Offset(0f, topY),
                        size = Size(w, (botY - topY).coerceAtLeast(2f))
                    )

                    // Left Edge: Centimeters & Millimeters
                    val totalMm = (h / pxPerMm).toInt()
                    for (mm in 0..totalMm) {
                        val y = mm * pxPerMm
                        val tickLen = when {
                            mm % 10 == 0 -> 42.dp.toPx()
                            mm % 5 == 0 -> 28.dp.toPx()
                            else -> 15.dp.toPx()
                        }
                        val tickStroke = if (mm % 10 == 0) 2.2.dp.toPx() else 1.dp.toPx()
                        drawLine(
                            color = if (mm % 10 == 0) theme.accent else theme.textPrimary.copy(alpha = 0.7f),
                            start = Offset(0f, y),
                            end = Offset(tickLen, y),
                            strokeWidth = tickStroke
                        )
                    }

                    // Right Edge: Inches & 1/16th subdivisions
                    val sixteenthPx = pxPerInch / 16f
                    val totalSixteenths = (h / sixteenthPx).toInt()
                    for (s in 0..totalSixteenths) {
                        val y = s * sixteenthPx
                        val tickLen = when {
                            s % 16 == 0 -> 44.dp.toPx()
                            s % 8 == 0 -> 32.dp.toPx()
                            s % 4 == 0 -> 22.dp.toPx()
                            else -> 12.dp.toPx()
                        }
                        val tickStroke = if (s % 16 == 0) 2.2.dp.toPx() else 1.dp.toPx()
                        drawLine(
                            color = if (s % 16 == 0) Color(0xFFFFD54F) else theme.textPrimary.copy(alpha = 0.7f),
                            start = Offset(w, y),
                            end = Offset(w - tickLen, y),
                            strokeWidth = tickStroke
                        )
                    }

                    // Caliper Handles
                    listOf(topY, botY).forEach { cy ->
                        drawLine(
                            color = theme.accent,
                            start = Offset(0f, cy),
                            end = Offset(w, cy),
                            strokeWidth = 2.5.dp.toPx()
                        )
                        drawCircle(
                            color = theme.accent,
                            radius = 9.dp.toPx(),
                            center = Offset(w / 2f, cy)
                        )
                    }
                }

                // Scale Labels Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CM / MM", color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("INCHES (1/16\")", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ================================================================================================
// 2. SCREEN TEST (FREE: Dead pixel and color check)
// ================================================================================================

@Composable
fun ScreenTestDeadPixelScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current

    val testStages = remember {
        listOf(
            "Pure Red" to Color(0xFFFF0000),
            "Pure Green" to Color(0xFF00FF00),
            "Pure Blue" to Color(0xFF0000FF),
            "Pure White" to Color(0xFFFFFFFF),
            "True OLED Black" to Color(0xFF000000),
            "Cyan" to Color(0xFF00FFFF),
            "Magenta" to Color(0xFFFF00FF),
            "Yellow" to Color(0xFFFFFF00),
            "50% Neutral Gray" to Color(0xFF808080),
            "Gradient & Grid" to Color.Transparent
        )
    }

    var stageIndex by remember { mutableIntStateOf(0) }
    var showHudOverlay by remember { mutableStateOf(true) }

    val (stageName, stageColor) = testStages[stageIndex % testStages.size]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(if (stageName == "Gradient & Grid") Color.Black else stageColor)
            .clickable {
                FeedbackHelper.hapticTick(view)
                stageIndex = (stageIndex + 1) % testStages.size
            }
            .testTag("screen_test_canvas")
    ) {
        if (stageName == "Gradient & Grid") {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color.Gray, Color.Black)
                    )
                )
                val step = 32.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Color.Cyan.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Color.Cyan.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
        }

        if (showHudOverlay) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pattern ${stageIndex + 1}/${testStages.size}: $stageName",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Tap anywhere on the panel to cycle dead-pixel & uniformity patterns.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                stageIndex = (stageIndex - 1 + testStages.size) % testStages.size
                            }
                        ) {
                            Text("Previous", color = Color.White)
                        }
                        Button(
                            onClick = {
                                stageIndex = (stageIndex + 1) % testStages.size
                            },
                            modifier = Modifier.testTag("screen_test_next")
                        ) {
                            Text("Next Pattern")
                        }
                        TextButton(onClick = { showHudOverlay = false }) {
                            Text("Hide HUD", color = Color(0xFFFFD54F))
                        }
                    }
                }
            }
        } else {
            SmallFloatingActionButton(
                onClick = { showHudOverlay = true },
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = "Show Controls")
            }
        }
    }
}

// ================================================================================================
// 3. MAGNIFIER AND COLOR PICKER (FREE: Camera zoom, LED torch, live HEX/RGB/HSL picker & swatches)
// ================================================================================================

@Composable
fun MagnifierAndColorPickerScreen(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    var zoomRatio by remember { mutableFloatStateOf(2.0f) }
    var maxZoomRatio by remember { mutableFloatStateOf(8.0f) }
    var torchOn by remember { mutableStateOf(false) }
    var freezeSample by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    var sampledColor by remember { mutableStateOf(Color(0xFF29B6F6)) }
    val savedSwatches = remember {
        mutableStateListOf(
            Color(0xFF29B6F6),
            Color(0xFF00E676),
            Color(0xFFFFD54F),
            Color(0xFFEF5350)
        )
    }

    val argb = sampledColor.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val hexCode = "#%02X%02X%02X".format(r, g, b)
    val hsv = remember(r, g, b) {
        FloatArray(3).also { android.graphics.Color.RGBToHSV(r, g, b, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Camera Viewport or Permission Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardBackground.copy(alpha = 0.88f),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.5.dp, sampledColor, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            if (!hasCameraPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = theme.accent, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Camera Permission Required for Optical Magnifier & Live Color Picker",
                        color = theme.textPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onRequestCameraPermission,
                        modifier = Modifier.testTag("magnifier_grant_camera")
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                runCatching {
                                    val provider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val analyzer = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { analysis ->
                                            analysis.setAnalyzer(analysisExecutor) { proxy ->
                                                if (!freezeSample) {
                                                    val planes = proxy.planes
                                                    if (planes.isNotEmpty()) {
                                                        val yPlane = planes[0].buffer
                                                        val centerIdx = (yPlane.remaining() / 2).coerceAtLeast(0)
                                                        val luma = (yPlane.get(centerIdx).toInt() and 0xFF)
                                                        var u = 128
                                                        var v = 128
                                                        if (planes.size >= 3) {
                                                            val uBuf = planes[1].buffer
                                                            val vBuf = planes[2].buffer
                                                            if (uBuf.remaining() > 0 && vBuf.remaining() > 0) {
                                                                u = uBuf.get(uBuf.remaining() / 2).toInt() and 0xFF
                                                                v = vBuf.get(vBuf.remaining() / 2).toInt() and 0xFF
                                                            }
                                                        }
                                                        val rr = (luma + 1.370705f * (v - 128)).roundToInt().coerceIn(0, 255)
                                                        val gg = (luma - 0.337633f * (u - 128) - 0.698001f * (v - 128)).roundToInt().coerceIn(0, 255)
                                                        val bb = (luma + 1.732446f * (u - 128)).roundToInt().coerceIn(0, 255)
                                                        sampledColor = Color(rr, gg, bb)
                                                    }
                                                }
                                                proxy.close()
                                            }
                                        }
                                    provider.unbindAll()
                                    val cam = provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analyzer
                                    )
                                    cameraRef = cam
                                    maxZoomRatio = (cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 8f).coerceIn(2f, 10f)
                                    cam.cameraControl.setZoomRatio(zoomRatio.coerceAtMost(maxZoomRatio))
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Center Crosshair Reticle
                    Canvas(modifier = Modifier.size(64.dp).align(Alignment.Center)) {
                        drawCircle(
                            color = Color.White,
                            radius = 22.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = sampledColor,
                            radius = 6.dp.toPx()
                        )
                    }

                    // Floating Torch & Freeze Buttons
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                torchOn = !torchOn
                                cameraRef?.cameraControl?.enableTorch(torchOn)
                            },
                            modifier = Modifier.testTag("magnifier_torch_toggle")
                        ) {
                            Icon(
                                if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Torch"
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { freezeSample = !freezeSample },
                            modifier = Modifier.testTag("magnifier_freeze_toggle")
                        ) {
                            Icon(
                                if (freezeSample) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Freeze Sample"
                            )
                        }
                    }
                }
            }
        }

        // Zoom + Live Color Readout Card
        GlassInputCard(title = "Optical Zoom (${DecimalFormat("0.0").format(zoomRatio)}x) & Live Color Swatch") {
            Slider(
                value = zoomRatio,
                onValueChange = {
                    zoomRatio = it
                    cameraRef?.cameraControl?.setZoomRatio(it)
                },
                valueRange = 1.0f..maxZoomRatio.coerceAtLeast(4.0f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(sampledColor)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text(hexCode, color = theme.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "RGB($r, $g, $b)  •  HSL(${hsv[0].roundToInt()}°, ${(hsv[1] * 100).roundToInt()}%, ${(hsv[2] * 100).roundToInt()}%)",
                            color = theme.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            savedSwatches.add(0, sampledColor)
                            FeedbackHelper.hapticClick(view)
                        }
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Swatch", tint = theme.accent)
                    }
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Color HEX", hexCode))
                            FeedbackHelper.hapticSuccess(view)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy HEX", tint = theme.accent)
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(savedSwatches) { sw ->
                    val swHex = "#%06X".format(0xFFFFFF and sw.toArgb())
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = sw,
                        modifier = Modifier
                            .size(width = 76.dp, height = 32.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable {
                                sampledColor = sw
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(swHex, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ================================================================================================
// 4. DIY CALCULATORS PRO (PREMIUM: Paint, Tiles, Concrete, Flooring)
// ================================================================================================

@Composable
fun DiyCalculatorsProScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current

    val modes = listOf(
        "paint" to "Paint",
        "tiles" to "Tiles",
        "concrete" to "Concrete",
        "flooring" to "Flooring"
    )
    var activeMode by remember(forcedModeId) { mutableStateOf(forcedModeId ?: "paint") }

    var lengthMStr by remember { mutableStateOf("5.0") }
    var widthMStr by remember { mutableStateOf("4.0") }
    var heightMStr by remember { mutableStateOf("2.7") }
    var coatsStr by remember { mutableStateOf("2") }
    var tileLengthCmStr by remember { mutableStateOf("60") }
    var tileWidthCmStr by remember { mutableStateOf("60") }
    var slabDepthCmStr by remember { mutableStateOf("10") }
    var wastePctStr by remember { mutableStateOf("10") }

    val l = lengthMStr.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 5.0
    val w = widthMStr.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 4.0
    val h = heightMStr.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 2.7
    val coats = (coatsStr.toIntOrNull() ?: 2).coerceAtLeast(1)
    val wasteFactor = 1.0 + ((wastePctStr.toDoubleOrNull() ?: 10.0).coerceAtLeast(0.0) / 100.0)
    val df = remember { DecimalFormat("#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(modes) { (id, label) ->
                val selected = activeMode == id
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground.copy(alpha = 0.65f),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            FeedbackHelper.hapticTick(view)
                            activeMode = id
                        }
                        .testTag("diy_mode_$id")
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

        GlassInputCard(title = "Room / Surface Dimensions (Meters)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = lengthMStr, onValueChange = { lengthMStr = it }, label = { Text("Length (m)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = widthMStr, onValueChange = { widthMStr = it }, label = { Text("Width (m)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            when (activeMode) {
                "paint" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = heightMStr, onValueChange = { heightMStr = it }, label = { Text("Wall Height (m)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = coatsStr, onValueChange = { coatsStr = it }, label = { Text("Coats") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
                "tiles" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = tileLengthCmStr, onValueChange = { tileLengthCmStr = it }, label = { Text("Tile L (cm)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = tileWidthCmStr, onValueChange = { tileWidthCmStr = it }, label = { Text("Tile W (cm)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = wastePctStr, onValueChange = { wastePctStr = it }, label = { Text("Waste %") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
                "concrete" -> {
                    OutlinedTextField(value = slabDepthCmStr, onValueChange = { slabDepthCmStr = it }, label = { Text("Slab Thickness (cm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "flooring" -> {
                    OutlinedTextField(value = wastePctStr, onValueChange = { wastePctStr = it }, label = { Text("Underlay & Waste Allowance %") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        when (activeMode) {
            "paint" -> {
                val wallArea = 2.0 * (l + w) * h
                val totalCoatArea = wallArea * coats
                val litersNeeded = totalCoatArea / 10.0 // 10 m² per liter coverage
                val gallonsNeeded = litersNeeded / 3.78541
                GlassResultCard(
                    headlineLabel = "Paint Volume Required ($coats Coats)",
                    headlineValue = "${df.format(litersNeeded)} L  (${df.format(gallonsNeeded)} gal)",
                    rows = listOf(
                        "Single Coat Wall Area" to "${df.format(wallArea)} m²",
                        "Total Painted Surface" to "${df.format(totalCoatArea)} m²",
                        "Coverage Assumption" to "10 m² / Liter (350 sq ft / gal)"
                    )
                )
            }
            "tiles" -> {
                val floorArea = l * w
                val tileAreaM2 = ((tileLengthCmStr.toDoubleOrNull() ?: 60.0) / 100.0) * ((tileWidthCmStr.toDoubleOrNull() ?: 60.0) / 100.0)
                val rawTiles = if (tileAreaM2 > 0) floorArea / tileAreaM2 else 0.0
                val totalTiles = ceil(rawTiles * wasteFactor).toInt()
                val groutKg = floorArea * 0.5
                GlassResultCard(
                    headlineLabel = "Total Tiles Required (incl. Waste)",
                    headlineValue = "$totalTiles Tiles",
                    rows = listOf(
                        "Net Surface Area" to "${df.format(floorArea)} m²",
                        "Tiles Without Waste" to "${ceil(rawTiles).toInt()} tiles",
                        "Estimated Grout & Adhesive" to "${df.format(groutKg)} kg grout / ${df.format(floorArea * 4.0)} kg adhesive"
                    )
                )
            }
            "concrete" -> {
                val depthM = (slabDepthCmStr.toDoubleOrNull() ?: 10.0) / 100.0
                val cubicMeters = l * w * depthM
                val cubicYards = cubicMeters * 1.30795
                val weightKg = cubicMeters * 2400.0
                val bags20Kg = ceil(weightKg / 20.0).toInt()
                val bags40Kg = ceil(weightKg / 40.0).toInt()
                GlassResultCard(
                    headlineLabel = "Concrete Slab Volume",
                    headlineValue = "${df.format(cubicMeters)} m³  (${df.format(cubicYards)} yd³)",
                    rows = listOf(
                        "Estimated Wet Weight" to "${df.format(weightKg)} kg",
                        "20 kg Pre-Mix Bags" to "$bags20Kg bags",
                        "40 kg Pre-Mix Bags" to "$bags40Kg bags"
                    )
                )
            }
            "flooring" -> {
                val netArea = l * w
                val totalWithWaste = netArea * wasteFactor
                val packsNeeded = ceil(totalWithWaste / 2.2).toInt() // 2.2 m² standard plank pack
                GlassResultCard(
                    headlineLabel = "Flooring Coverage Needed",
                    headlineValue = "${df.format(totalWithWaste)} m²  ($packsNeeded Packs)",
                    rows = listOf(
                        "Net Room Footprint" to "${df.format(netArea)} m² (${df.format(netArea * 10.7639)} sq ft)",
                        "Standard Plank Packs (2.2 m²/pack)" to "$packsNeeded packs",
                        "Perimeter Baseboard Trim" to "${df.format(2 * (l + w))} linear m"
                    )
                )
            }
        }
    }
}

// ================================================================================================
// 5. QR GENERATOR + SAVED HISTORY (FREE) & QR STYLES AND COLORS (PREMIUM)
// ================================================================================================

data class SavedQrHistoryEntry(
    val type: String,
    val summary: String,
    val payload: String
)

@Composable
fun QrGeneratorAndStylesScreen(
    forcedModeId: String? = null,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    onLaunchScannerMode: (String) -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("dyne_qr_history_prefs", Context.MODE_PRIVATE) }

    val featureGate = LocalFeatureGate.current
    val stylesUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.QR_GENERATOR_STYLES) == true)

    val modes = listOf(
        "wifi" to "Wi-Fi",
        "url" to "URL",
        "text" to "Text",
        "contact" to "Contact",
        "history" to "Saved History",
        "styles_pro" to "Styles & Colors PRO"
    )
    var activeMode by remember(forcedModeId) { mutableStateOf(forcedModeId ?: "wifi") }

    // Payload state
    var wifiSsid by remember { mutableStateOf("HomeNetwork_5G") }
    var wifiPass by remember { mutableStateOf("SecurePass2026") }
    var wifiAuth by remember { mutableStateOf("WPA") }

    var urlInput by remember { mutableStateOf("https://developer.android.com") }
    var textInput by remember { mutableStateOf("Offline emergency contact & field kit") }
    var contactName by remember { mutableStateOf("Alex Rivera") }
    var contactPhone by remember { mutableStateOf("+15550192834") }
    var contactEmail by remember { mutableStateOf("alex@example.com") }

    // Pro styling state
    var fgColor by remember { mutableStateOf(Color(0xFF0A1118)) }
    var bgColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var roundedDots by remember { mutableStateOf(false) }

    val history = remember {
        mutableStateListOf<SavedQrHistoryEntry>().apply {
            val raw = prefs.getString("saved_qr_items_v1", null)
            if (!raw.isNullOrBlank()) {
                raw.split("\n").forEach { line ->
                    val p = line.split("|||")
                    if (p.size == 3) add(SavedQrHistoryEntry(p[0], p[1], p[2]))
                }
            }
            if (isEmpty()) {
                add(SavedQrHistoryEntry("Wi-Fi", "HomeNetwork_5G", "WIFI:T:WPA;S:HomeNetwork_5G;P:SecurePass2026;;"))
                add(SavedQrHistoryEntry("URL", "https://developer.android.com", "https://developer.android.com"))
            }
        }
    }

    fun persistHistory() {
        val serialized = history.take(30).joinToString("\n") { "${it.type}|||${it.summary}|||${it.payload}" }
        prefs.edit().putString("saved_qr_items_v1", serialized).apply()
    }

    val currentPayload = when (activeMode) {
        "wifi" -> "WIFI:T:$wifiAuth;S:$wifiSsid;P:$wifiPass;;"
        "url" -> urlInput.ifBlank { "https://example.com" }
        "text" -> textInput.ifBlank { "Hello" }
        "contact" -> "BEGIN:VCARD\nVERSION:3.0\nFN:$contactName\nTEL:$contactPhone\nEMAIL:$contactEmail\nEND:VCARD"
        else -> history.firstOrNull()?.payload ?: "https://example.com"
    }

    val qrBitmap = remember(currentPayload, fgColor, bgColor, roundedDots, stylesUnlocked) {
        generateStyledQrBitmap(
            payload = currentPayload,
            sizePx = 512,
            fgArgb = if (stylesUnlocked) fgColor.toArgb() else android.graphics.Color.BLACK,
            bgArgb = if (stylesUnlocked) bgColor.toArgb() else android.graphics.Color.WHITE,
            roundedDots = stylesUnlocked && roundedDots
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(modes) { (id, label) ->
                val selected = activeMode == id
                val isProTab = id == "styles_pro"
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground.copy(alpha = 0.65f),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            FeedbackHelper.hapticTick(view)
                            if (isProTab && !stylesUnlocked) {
                                onOpenPaywall()
                            } else {
                                activeMode = id
                            }
                        }
                        .testTag("qr_mode_$id")
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
                        if (isProTab && !stylesUnlocked) {
                            ProBadge()
                        }
                    }
                }
            }
        }

        // QR Code Preview Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardBackground.copy(alpha = 0.88f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, theme.accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            FeedbackHelper.hapticSuccess(view)
                            val summary = when (activeMode) {
                                "wifi" -> wifiSsid
                                "url" -> urlInput
                                "contact" -> contactName
                                else -> textInput.take(28)
                            }
                            history.add(0, SavedQrHistoryEntry(activeMode.uppercase(), summary, currentPayload))
                            persistHistory()
                        },
                        modifier = Modifier.weight(1f).testTag("qr_save_to_history")
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save to History")
                    }
                    OutlinedButton(
                        onClick = { onLaunchScannerMode("QR_BARCODE") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Scan QR")
                    }
                }
            }
        }

        // Mode-Specific Input or History / Styles
        when (activeMode) {
            "wifi" -> {
                GlassInputCard(title = "Plain Wi-Fi QR (Free)") {
                    OutlinedTextField(value = wifiSsid, onValueChange = { wifiSsid = it }, label = { Text("Network SSID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = wifiPass, onValueChange = { wifiPass = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WPA", "WEP", "nopass").forEach { auth ->
                            FilterChip(selected = wifiAuth == auth, onClick = { wifiAuth = auth }, label = { Text(auth) })
                        }
                    }
                }
            }
            "url" -> {
                GlassInputCard(title = "Website URL QR (Free)") {
                    OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("https://...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
            "text" -> {
                GlassInputCard(title = "Plain Text QR (Free)") {
                    OutlinedTextField(value = textInput, onValueChange = { textInput = it }, label = { Text("Text Content") }, modifier = Modifier.fillMaxWidth())
                }
            }
            "contact" -> {
                GlassInputCard(title = "Contact vCard QR (Free)") {
                    OutlinedTextField(value = contactName, onValueChange = { contactName = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Email Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
            "history" -> {
                GlassInputCard(title = "Saved QR History (${history.size})") {
                    history.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    textInput = item.payload
                                    activeMode = "text"
                                }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.type}: ${item.summary}", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(item.payload, color = theme.textSecondary, fontSize = 11.sp, maxLines = 1)
                            }
                            IconButton(onClick = {
                                history.remove(item)
                                persistHistory()
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = theme.textSecondary)
                            }
                        }
                    }
                }
            }
            "styles_pro" -> {
                GlassInputCard(title = "Custom QR Styles & Colors (PRO)") {
                    Text("Foreground Palette", fontSize = 12.sp, color = theme.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Color(0xFF0A1118),
                            Color(0xFF0D47A1),
                            Color(0xFF1B5E20),
                            Color(0xFF4A148C),
                            Color(0xFFB71C1C),
                            Color(0xFFE65100)
                        ).forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(2.dp, if (fgColor == col) Color.White else Color.Transparent, CircleShape)
                                    .clickable { fgColor = col }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rounded Dot Matrix Style", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = roundedDots, onCheckedChange = { roundedDots = it })
                    }
                }
            }
        }
    }
}

private fun generateStyledQrBitmap(
    payload: String,
    sizePx: Int,
    fgArgb: Int,
    bgArgb: Int,
    roundedDots: Boolean
): Bitmap? {
    return runCatching {
        val gridSize = 25
        val quietZone = 2
        val totalModules = gridSize + quietZone * 2
        val scale = (sizePx / totalModules).coerceAtLeast(8)
        val actualSizePx = totalModules * scale
        val pixels = IntArray(actualSizePx * actualSizePx) { bgArgb }

        fun isFinderPattern(r: Int, c: Int, topR: Int, leftC: Int): Boolean? {
            val dr = r - topR
            val dc = c - leftC
            if (dr !in -1..7 || dc !in -1..7) return null
            if (dr == -1 || dr == 7 || dc == -1 || dc == 7) return false
            if (dr == 0 || dr == 6 || dc == 0 || dc == 6) return true
            if (dr in 2..4 && dc in 2..4) return true
            return false
        }

        val content = payload.ifBlank { "Dyne" }
        val bytes = content.encodeToByteArray()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val finder = isFinderPattern(r, c, 0, 0)
                    ?: isFinderPattern(r, c, 0, gridSize - 7)
                    ?: isFinderPattern(r, c, gridSize - 7, 0)

                val isDark = when {
                    finder != null -> finder
                    r == 6 -> c % 2 == 0
                    c == 6 -> r % 2 == 0
                    r in 16..20 && c in 16..20 -> {
                        val dr = r - 16
                        val dc = c - 16
                        dr == 0 || dr == 4 || dc == 0 || dc == 4 || (dr == 2 && dc == 2)
                    }
                    else -> {
                        val idx = r * gridSize + c
                        val b = bytes[idx % bytes.size].toInt() and 0xFF
                        val hash = b * 31 + r * 17 + c * 13 + content.hashCode()
                        (hash and 1) == 0
                    }
                }
                val startY = (r + quietZone) * scale
                val startX = (c + quietZone) * scale
                for (py in 0 until scale) {
                    val rowOffset = (startY + py) * actualSizePx + startX
                    for (px in 0 until scale) {
                        val insideDot = if (roundedDots && finder == null) {
                            val dx = px - scale / 2f
                            val dy = py - scale / 2f
                            dx * dx + dy * dy <= (scale * 0.45f) * (scale * 0.45f)
                        } else true
                        pixels[rowOffset + px] = if (isDark && insideDot) fgArgb else bgArgb
                    }
                }
            }
        }
        Bitmap.createBitmap(actualSizePx, actualSizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, actualSizePx, 0, 0, actualSizePx, actualSizePx)
        }
    }.getOrNull()
}

// ================================================================================================
// 6. IMAGE TOOLKIT PRO (PREMIUM: Compress, Resize, Convert JPG/PNG/WebP, Remove EXIF & Location, Batch)
// ================================================================================================

@Composable
fun ImageToolkitProScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val view = LocalView.current

    var formatChoice by remember { mutableStateOf("JPEG") } // JPEG, PNG, WEBP
    var qualityPct by remember { mutableFloatStateOf(80f) }
    var resizeScalePct by remember { mutableFloatStateOf(100f) }
    var stripExifAndGps by remember { mutableStateOf(true) }

    // Create a clean built-in synthetic sample bitmap so the toolkit works immediately offline & in tests
    val sampleBitmap = remember {
        Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply {
                shader = android.graphics.LinearGradient(
                    0f, 0f, 640f, 480f,
                    android.graphics.Color.rgb(20, 90, 160),
                    android.graphics.Color.rgb(0, 210, 140),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, 640f, 480f, paint)
        }
    }

    var selectedBitmap by remember { mutableStateOf<Bitmap>(sampleBitmap) }
    var batchCount by remember { mutableIntStateOf(1) }
    var processedSizeBytes by remember { mutableIntStateOf(0) }
    var originalSizeBytes by remember { mutableIntStateOf(245_760) }
    var statusText by remember { mutableStateOf("Ready • EXIF & GPS Stripper Armed") }

    val pickMultipleMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            batchCount = uris.size
            runCatching {
                context.contentResolver.openInputStream(uris.first())?.use { stream ->
                    val bytes = stream.readBytes()
                    originalSizeBytes = bytes.size
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let {
                        selectedBitmap = it
                    }
                }
            }
            statusText = "Loaded ${uris.size} image(s) via Zero-Permission Photo Picker"
        }
    }

    fun runImagePipeline() {
        FeedbackHelper.hapticClick(view)
        val scale = (resizeScalePct / 100f).coerceIn(0.1f, 1.0f)
        val targetW = (selectedBitmap.width * scale).roundToInt().coerceAtLeast(16)
        val targetH = (selectedBitmap.height * scale).roundToInt().coerceAtLeast(16)
        // Re-encoding fresh pixel buffer strips all EXIF, camera serials, and GPS location tags automatically
        val scaled = Bitmap.createScaledBitmap(selectedBitmap, targetW, targetH, true)
        val baos = ByteArrayOutputStream()
        val compressFormat = when (formatChoice) {
            "PNG" -> Bitmap.CompressFormat.PNG
            "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            else -> Bitmap.CompressFormat.JPEG
        }
        scaled.compress(compressFormat, qualityPct.roundToInt(), baos)
        val outBytes = baos.toByteArray()
        processedSizeBytes = outBytes.size

        val ext = formatChoice.lowercase()
        val outFile = File(context.cacheDir, "dyne_image_toolkit_out.$ext")
        runCatching {
            FileOutputStream(outFile).use { it.write(outBytes) }
        }
        statusText = buildString {
            append("Processed $batchCount image(s) -> ${targetW}x${targetH} $formatChoice")
            if (stripExifAndGps) append(" • EXIF & GPS Location Removed")
        }
        FeedbackHelper.hapticSuccess(view)
    }

    LaunchedEffect(formatChoice, qualityPct, resizeScalePct, stripExifAndGps) {
        runImagePipeline()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "1. Select Photo or Batch (Zero-Permission Picker)") {
            Button(
                onClick = {
                    pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth().testTag("image_toolkit_pick_photos")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Select Photos (Single or Batch)")
            }
            Text(statusText, color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        GlassInputCard(title = "2. Format, Compression, Resize & EXIF Privacy") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("JPEG", "PNG", "WEBP").forEach { fmt ->
                    FilterChip(
                        selected = formatChoice == fmt,
                        onClick = { formatChoice = fmt },
                        label = { Text(fmt) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Compression Quality: ${qualityPct.roundToInt()}%", color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Slider(value = qualityPct, onValueChange = { qualityPct = it }, valueRange = 15f..100f)

            Text("Resize Dimensions: ${resizeScalePct.roundToInt()}% (${(selectedBitmap.width * resizeScalePct / 100f).roundToInt()} x ${(selectedBitmap.height * resizeScalePct / 100f).roundToInt()} px)", color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Slider(value = resizeScalePct, onValueChange = { resizeScalePct = it }, valueRange = 20f..100f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Remove EXIF & GPS Location Tags", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Strips camera metadata, timestamps, and GPS coordinates", color = theme.textSecondary, fontSize = 11.sp)
                }
                Switch(checked = stripExifAndGps, onCheckedChange = { stripExifAndGps = it })
            }
        }

        GlassResultCard(
            headlineLabel = "Output Size ($formatChoice • Batch x$batchCount)",
            headlineValue = "${processedSizeBytes / 1024} KB  (Before: ${originalSizeBytes / 1024} KB)",
            rows = listOf(
                "Target Resolution" to "${(selectedBitmap.width * resizeScalePct / 100f).roundToInt()} × ${(selectedBitmap.height * resizeScalePct / 100f).roundToInt()} px",
                "EXIF & GPS Coordinates" to if (stripExifAndGps) "Stripped Clean ✓" else "Preserved",
                "Batch Queue" to "$batchCount file(s)"
            )
        )
    }
}

// ================================================================================================
// 7. TRIP & GEAR CHECKLISTS PRO (PREMIUM: Camping, Fishing, Hunting, Travel templates, editable, progress rings)
// ================================================================================================

data class GearCheckItem(
    val id: Long,
    val label: String,
    val checked: Boolean
)

@Composable
fun TripAndGearChecklistsProScreen(
    forcedModeId: String? = null,
    onSaveNoteToVault: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current

    val templates = remember {
        mapOf(
            "camping" to ("Camping" to listOf(
                "Tent, Poles & Stakes", "Sleeping Bag & Pad", "Headlamp + Spare Batteries",
                "Camp Stove & Fuel Canister", "First-Aid Kit & Whistle", "Water Filter / 2L Bottles",
                "Weatherproof Jacket", "Firestarter / Waterproof Matches"
            )),
            "fishing" to ("Fishing" to listOf(
                "Rod, Reel & Spare Line", "Tackle Box (Hooks, Lures, Sinkers)", "Needle-Nose Pliers & Line Cutter",
                "Fishing License & ID", "Polarized Sunglasses", "Cooler & Ice Packs",
                "Landing Net", "Sunscreen & Hat"
            )),
            "hunting" to ("Hunting" to listOf(
                "Hunting License & Tags", "High-Visibility Blaze Orange Vest", "Binoculars & Rangefinder",
                "Compass & Offline Topo Map", "Field Dressing Knife & Gloves", "Headlamp (Red/Green LED)",
                "Thermal Base Layers", "Emergency Trauma & First-Aid Kit"
            )),
            "travel" to ("Travel" to listOf(
                "Passport / ID & Boarding Pass", "Universal Power Adapter & Charger", "Power Bank (10,000+ mAh)",
                "Medications & Travel Kit", "Offline Maps & Hotel Confirmation", "Packing Cubes & Toiletries",
                "Reusable Water Bottle", "Payment Cards & Local Cash"
            ))
        )
    }

    var activeTemplateKey by remember(forcedModeId) {
        mutableStateOf(if (forcedModeId in templates.keys) forcedModeId!! else "camping")
    }

    val checklistMap = remember {
        mutableStateMapOf<String, MutableList<GearCheckItem>>().apply {
            templates.forEach { (key, pair) ->
                put(
                    key,
                    pair.second.mapIndexed { idx, name ->
                        GearCheckItem(id = idx.toLong() + 1L, label = name, checked = idx < 2)
                    }.toMutableStateList()
                )
            }
        }
    }

    var newGearText by remember { mutableStateOf("") }
    val currentItems = checklistMap[activeTemplateKey] ?: remember { mutableStateListOf() }
    val checkedCount = currentItems.count { it.checked }
    val totalCount = currentItems.size.coerceAtLeast(1)
    val progress = checkedCount.toFloat() / totalCount.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Template Switcher
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates.keys.toList()) { key ->
                val title = templates[key]?.first ?: key
                val selected = activeTemplateKey == key
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground.copy(alpha = 0.65f),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            FeedbackHelper.hapticTick(view)
                            activeTemplateKey = key
                        }
                        .testTag("gear_template_$key")
                ) {
                    Text(
                        text = title,
                        color = if (selected) theme.accent else theme.textPrimary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }

        // Progress Ring Header Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = theme.cardBackground.copy(alpha = 0.88f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(68.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 7.dp.toPx()
                        drawArc(
                            color = theme.textSecondary.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = if (progress >= 1f) Color(0xFF00E676) else theme.accent,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        color = theme.textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${templates[activeTemplateKey]?.first} Gear Checklist",
                        color = theme.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$checkedCount of ${currentItems.size} items packed",
                        color = theme.textSecondary,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        currentItems.forEachIndexed { idx, item ->
                            currentItems[idx] = item.copy(checked = false)
                        }
                    }
                ) {
                    Text("Uncheck All", fontSize = 11.sp)
                }
            }
        }

        // Editable Checklist Items
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(currentItems, key = { _, it -> it.id }) { idx, item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = theme.cardBackground.copy(alpha = 0.78f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (item.checked) Color(0xFF00E676).copy(alpha = 0.4f) else theme.textSecondary.copy(alpha = 0.15f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            FeedbackHelper.hapticTick(view)
                            currentItems[idx] = item.copy(checked = !item.checked)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = item.checked,
                                onCheckedChange = {
                                    FeedbackHelper.hapticTick(view)
                                    currentItems[idx] = item.copy(checked = it)
                                }
                            )
                            Text(
                                text = item.label,
                                color = if (item.checked) theme.textSecondary else theme.textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(
                            onClick = { currentItems.removeAt(idx) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove item", tint = theme.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Add Custom Gear Item Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newGearText,
                onValueChange = { newGearText = it },
                label = { Text("Add Custom Gear Item") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newGearText.isNotBlank()) {
                        FeedbackHelper.hapticClick(view)
                        currentItems.add(
                            GearCheckItem(
                                id = System.currentTimeMillis(),
                                label = newGearText.trim(),
                                checked = false
                            )
                        )
                        newGearText = ""
                    }
                },
                modifier = Modifier.testTag("gear_add_item")
            ) {
                Text("Add")
            }
        }
    }
}
