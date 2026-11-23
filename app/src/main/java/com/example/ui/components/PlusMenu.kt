package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class PlusMenuEntranceStyle {
    STACK,
    FAN,
    SPIRAL
}

data class PlusMenuItemSpec(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Animated Liquid Glass PlusMenu stacked above the raised center Scan button.
 * - Supports 3 random entrance styles (Stack, Fan, Spiral) that all settle in the same vertical stack.
 * - Item nearest the button appears first, each next one 50ms later:
 *   fade-in 150ms + scale from 0.5 with DampingRatioMediumBouncy spring + slide up.
 * - Exit is faster: 100ms fade, scale to 0.5, slide down.
 * - Tapping an item closes the menu first, then navigates.
 */
@Composable
fun ScanPlusMenuOverlay(
    visible: Boolean,
    shortcutIds: List<String>,
    hazeState: HazeState?,
    onRequestClose: () -> Unit,
    onSelectDocumentScan: () -> Unit,
    onSelectQrBarcodeScan: () -> Unit,
    onOpenToolShortcut: (String) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val view = LocalView.current
    val tapSound = LocalTapSound.current
    val scope = rememberCoroutineScope()

    val specs = remember(shortcutIds) {
        val ids = shortcutIds.ifEmpty {
            listOf("scan_doc", "scan_qr", "quick_note", "flashlight")
        }.take(4)
        ids.mapNotNull { id -> resolvePlusMenuSpec(id) }
            .ifEmpty {
                listOf(
                    resolvePlusMenuSpec("scan_doc")!!,
                    resolvePlusMenuSpec("scan_qr")!!,
                    resolvePlusMenuSpec("quick_note")!!,
                    resolvePlusMenuSpec("flashlight")!!
                )
            }
    }

    var keepComposed by remember { mutableStateOf(visible) }
    var entranceStyle by remember { mutableStateOf(PlusMenuEntranceStyle.STACK) }
    var isClosing by remember { mutableStateOf(false) }

    val scrimAlpha = remember { Animatable(0f) }
    val itemCount = specs.size.coerceAtLeast(1)
    val itemAlphas = remember(itemCount) { List(itemCount) { Animatable(0f) } }
    val itemScales = remember(itemCount) { List(itemCount) { Animatable(0.5f) } }
    val itemProgress = remember(itemCount) { List(itemCount) { Animatable(0f) } } // 0 = at button, 1 = settled

    suspend fun animateMenuOut() {
        if (theme.reduceMotion) {
            scrimAlpha.animateTo(0f, tween(100))
            return
        }
        coroutineScope {
            launch {
                scrimAlpha.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
            }
            for (i in 0 until itemCount) {
                launch {
                    itemAlphas[i].animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                }
                launch {
                    itemScales[i].animateTo(0.5f, tween(100, easing = FastOutSlowInEasing))
                }
                launch {
                    itemProgress[i].animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    val handleDismissWithSound = {
        if (!isClosing) {
            isClosing = true
            tapSound?.playTap(view)
            scope.launch {
                animateMenuOut()
                keepComposed = false
                isClosing = false
                onRequestClose()
            }
        }
    }

    val handleItemClick: (PlusMenuItemSpec) -> Unit = { spec ->
        if (!isClosing) {
            isClosing = true
            tapSound?.playTap(view)
            scope.launch {
                // Close the menu first (100ms fast exit), then navigate
                animateMenuOut()
                keepComposed = false
                isClosing = false
                onRequestClose()
                when (spec.id) {
                    "scan_doc" -> onSelectDocumentScan()
                    "scan_qr" -> onSelectQrBarcodeScan()
                    else -> onOpenToolShortcut(spec.id)
                }
            }
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            isClosing = false
            keepComposed = true
            val styles = PlusMenuEntranceStyle.entries
            entranceStyle = styles[Random.nextInt(styles.size)]

            // Reset initial states before entrance
            scrimAlpha.snapTo(0f)
            for (i in 0 until itemCount) {
                itemAlphas[i].snapTo(0f)
                itemScales[i].snapTo(0.5f)
                itemProgress[i].snapTo(0f)
            }

            launch {
                scrimAlpha.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
            }

            if (theme.reduceMotion) {
                for (i in 0 until itemCount) {
                    itemAlphas[i].snapTo(1f)
                    itemScales[i].snapTo(1f)
                    itemProgress[i].snapTo(1f)
                }
            } else {
                // Item nearest the button (last in list) appears first, each next one 50ms later
                for (i in (itemCount - 1) downTo 0) {
                    val orderFromBottom = (itemCount - 1) - i
                    launch {
                        delay(orderFromBottom * 50L)
                        launch {
                            itemAlphas[i].animateTo(1f, tween(150, easing = FastOutSlowInEasing))
                        }
                        launch {
                            itemScales[i].animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                        itemProgress[i].animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            }
        } else if (keepComposed && !isClosing) {
            isClosing = true
            animateMenuOut()
            keepComposed = false
            isClosing = false
        }
    }

    if (!keepComposed) return

    BackHandler(enabled = keepComposed) {
        handleDismissWithSound()
    }

    val isAmoledMono = theme.name.equals("AMOLED Mono", ignoreCase = true)
    val paletteColors = remember(theme, isAmoledMono) {
        if (isAmoledMono) {
            listOf(Color.White, Color.Black, Color.White, Color.Black)
        } else {
            listOf(
                theme.bentoCoral,
                theme.bentoSky,
                theme.bentoLime,
                theme.bentoLavender
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scan_plus_menu_overlay")
    ) {
        // Blurred, dimmed scrim behind menu items; tapping outside closes the menu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha.value }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = if (theme.isDark) 0.56f else 0.36f),
                            Color.Black.copy(alpha = if (theme.isDark) 0.76f else 0.52f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = handleDismissWithSound
                )
        )

        // Stacked Glass Pill Menu Items above the raised center Scan button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 108.dp)
        ) {
            specs.forEachIndexed { index, spec ->
                val p = itemProgress[index].value
                val inv = 1f - p
                val distanceFromBottom = (specs.size - index)

                // Compute entrance trajectory based on random entranceStyle (all settle at 0, 0, 0deg when p == 1f)
                val slideY = (distanceFromBottom * 38f + 22f) * inv
                val slideX = when (entranceStyle) {
                    PlusMenuEntranceStyle.STACK -> 0f
                    PlusMenuEntranceStyle.FAN -> {
                        val direction = if (index % 2 == 0) -1f else 1f
                        sin(inv * PI.toFloat() * 0.85f) * 64f * direction
                    }
                    PlusMenuEntranceStyle.SPIRAL -> {
                        val angle = inv * PI.toFloat() * 1.35f + index * 0.7f
                        cos(angle) * 58f * inv
                    }
                }
                val itemRotation = when (entranceStyle) {
                    PlusMenuEntranceStyle.STACK -> 0f
                    PlusMenuEntranceStyle.FAN -> {
                        val direction = if (index % 2 == 0) -1f else 1f
                        inv * 22f * direction
                    }
                    PlusMenuEntranceStyle.SPIRAL -> {
                        val direction = if (index % 2 == 0) -1f else 1f
                        inv * 75f * direction
                    }
                }

                val circleBg = paletteColors[index % paletteColors.size]
                val iconTint = if (isAmoledMono) {
                    if (circleBg == Color.White) Color.Black else Color.White
                } else {
                    BentoTextBlack
                }

                GlassSurface(
                    hazeState = hazeState,
                    shape = RoundedCornerShape(30.dp),
                    cornerRadius = 30.dp,
                    fillAlpha = 0.20f,
                    tintColor = if (theme.isDark) {
                        Color(0xFF1A1B20).copy(alpha = 0.90f)
                    } else {
                        Color.White.copy(alpha = 0.92f)
                    },
                    enableEdgeRefraction = true,
                    shadowElevation = 18.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = slideX.dp.roundToPx(),
                                y = slideY.dp.roundToPx()
                            )
                        }
                        .graphicsLayer {
                            alpha = itemAlphas[index].value
                            scaleX = itemScales[index].value
                            scaleY = itemScales[index].value
                            rotationZ = itemRotation
                        }
                        .widthIn(min = 216.dp)
                        .dyneClickable(
                            targetScale = 0.95f,
                            testTag = spec.testTag,
                            onClick = { handleItemClick(spec) }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(circleBg)
                                .then(
                                    if (isAmoledMono) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = if (circleBg == Color.Black) Color.White else Color.Black,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = spec.label,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = spec.label,
                            style = type.label.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

private fun resolvePlusMenuSpec(id: String): PlusMenuItemSpec? {
    return when (id) {
        "scan_doc" -> PlusMenuItemSpec(
            id = "scan_doc",
            label = "Scan Document",
            icon = Icons.Default.DocumentScanner,
            testTag = "scan_option_document"
        )
        "scan_qr" -> PlusMenuItemSpec(
            id = "scan_qr",
            label = "Scan QR / Barcode",
            icon = Icons.Default.QrCodeScanner,
            testTag = "scan_option_qr_barcode"
        )
        "quick_note" -> PlusMenuItemSpec(
            id = "quick_note",
            label = "Quick Note",
            icon = Icons.Default.EditNote,
            testTag = "scan_option_quick_note"
        )
        "flashlight" -> PlusMenuItemSpec(
            id = "flashlight",
            label = "Flashlight",
            icon = Icons.Default.FlashlightOn,
            testTag = "scan_option_flashlight"
        )
        "unit_converter" -> PlusMenuItemSpec(
            id = "unit_converter",
            label = "Unit Converter",
            icon = Icons.Default.SwapHoriz,
            testTag = "scan_option_unit_converter"
        )
        "world_clock" -> PlusMenuItemSpec(
            id = "world_clock",
            label = "World Clock",
            icon = Icons.Default.Public,
            testTag = "scan_option_world_clock"
        )
        "ruler" -> PlusMenuItemSpec(
            id = "ruler",
            label = "Screen Ruler",
            icon = Icons.Default.Straighten,
            testTag = "scan_option_ruler"
        )
        "sound_db" -> PlusMenuItemSpec(
            id = "sound_db",
            label = "Sound Meter",
            icon = Icons.Default.GraphicEq,
            testTag = "scan_option_sound_db"
        )
        else -> null
    }
}
