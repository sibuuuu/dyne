package com.example.ui.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.data.DocumentRecord
import com.example.data.NoteRecord
import com.example.data.PdfPageSize
import com.example.ui.components.DyneSegmentedControl
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class NoteColorOption(
    val hex: String,
    val name: String,
    val color: Color
)

val NotePaletteColors = listOf(
    NoteColorOption("#C6F27A", "Lime", Color(0xFFC6F27A)),
    NoteColorOption("#38BDF8", "Sky", Color(0xFF38BDF8)),
    NoteColorOption("#FB7185", "Coral", Color(0xFFFB7185)),
    NoteColorOption("#818CF8", "Indigo", Color(0xFF818CF8)),
    NoteColorOption("#A78BFA", "Violet", Color(0xFFA78BFA)),
    NoteColorOption("#34D399", "Emerald", Color(0xFF34D399)),
    NoteColorOption("#F472B6", "Rose", Color(0xFFF472B6)),
    NoteColorOption("#F59E0B", "Amber", Color(0xFFF59E0B)),
    NoteColorOption("#2DD4BF", "Teal", Color(0xFF2DD4BF)),
    NoteColorOption("#60A5FA", "Cobalt", Color(0xFF60A5FA)),
    NoteColorOption("#E879F9", "Orchid", Color(0xFFE879F9)),
    NoteColorOption("#E2E8F0", "Platinum", Color(0xFFE2E8F0))
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFFC6F27A)): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}

private fun authenticateNoteBiometrics(
    context: Context,
    title: String,
    subtitle: String,
    onAuthenticated: () -> Unit,
    onMessage: (String) -> Unit
) {
    fun findFragmentActivity(ctx: Context): FragmentActivity? {
        var cur: Context? = ctx
        while (cur is ContextWrapper) {
            if (cur is FragmentActivity) return cur
            cur = cur.baseContext
        }
        return null
    }

    val activity = findFragmentActivity(context)
    val biometricManager = BiometricManager.from(context)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    if (activity == null || biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
        onAuthenticated()
        return
    }

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
                onMessage(errString.toString())
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(authenticators)
        .build()
    prompt.authenticate(info)
}

/**
 * Rebuilt Notes Workspace Section:
 * - Segmented control: Text | Checklists | Drawing
 * - Text: search, 2-column grid of note cards (color stripe, title, 3-line preview, pin, date),
 *   and an accent "+" that opens a full-screen editor (title, body, color, pin, tags, auto-save, share, delete).
 * - Checklists: items with check, drag to reorder, progress ring.
 * - Drawing: a grid of sketch thumbnails and "+ New sketch" opening the Full-Screen Drawing Pad route.
 * - Free templates: Shopping list, Meeting notes, Trip checklist, Daily plan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesWorkspaceSection(
    scanViewModel: ScanViewModel,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    hazeState: HazeState? = null,
    showLibraryJumpButton: Boolean = false,
    onJumpToLibraryNotes: (() -> Unit)? = null,
    forcedModeId: String? = null
) {
    val context = LocalContext.current
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

    val searchQuery by scanViewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredNotes by scanViewModel.filteredNotes.collectAsStateWithLifecycle()
    val allNotes by scanViewModel.allNotes.collectAsStateWithLifecycle()

    // 0 = Text, 1 = Checklists, 2 = Drawing
    var modeIndex by rememberSaveable {
        mutableIntStateOf(
            when (forcedModeId) {
                "checklist" -> 1
                "sketch" -> 2
                else -> 0
            }
        )
    }
    LaunchedEffect(forcedModeId) {
        when (forcedModeId) {
            "text_note" -> modeIndex = 0
            "checklist" -> modeIndex = 1
            "sketch" -> modeIndex = 2
        }
    }

    var selectedSmartFolder by rememberSaveable { mutableStateOf("All") }
    var selectedTagChip by rememberSaveable { mutableStateOf("All") }
    var notePendingPdfExport by remember { mutableStateOf<NoteRecord?>(null) }
    var selectedPdfPageSize by remember { mutableStateOf(PdfPageSize.defaultForLocale()) }

    val allNonDrawingCount = remember(allNotes) { allNotes.count { !it.isDrawing } }
    val allSketchesCount = remember(allNotes) { allNotes.count { it.isDrawing } }

    val availableFolders = remember(allNotes) {
        listOf("All", "Pinned", "Locked", "Reminders") +
            allNotes.mapNotNull { it.folderName.takeIf { f -> f.isNotBlank() } }.distinct()
    }
    val availableTags = remember(allNotes) {
        listOf("All") + allNotes.flatMap { it.tagsList }.distinct()
    }

    val scopedNotes = remember(filteredNotes, selectedSmartFolder, selectedTagChip) {
        filteredNotes.filter { note ->
            val folderOk = when (selectedSmartFolder) {
                "All" -> true
                "Pinned" -> note.isPinned
                "Locked" -> note.isLocked
                "Reminders" -> note.reminderEpochMs > 0L
                else -> note.folderName.equals(selectedSmartFolder, ignoreCase = true)
            }
            val tagOk = selectedTagChip == "All" ||
                note.tagsList.any { it.equals(selectedTagChip, ignoreCase = true) }
            folderOk && tagOk
        }
    }

    val textNotes = remember(scopedNotes) {
        scopedNotes.filter { !it.isDrawing && it.noteType != "CHECKLIST" }
    }
    val checklistNotes = remember(scopedNotes) {
        scopedNotes.filter { !it.isDrawing && it.noteType == "CHECKLIST" }
    }
    val drawingNotes = remember(scopedNotes) {
        scopedNotes.filter { it.isDrawing || it.noteType == "DRAWING" }
    }

    fun tryCreateOrOpenNote(template: FreeNoteTemplate? = null, forceChecklist: Boolean = false) {
        if (!isNotesUnlocked && allNonDrawingCount >= 5) {
            requireNotesPro(
                title = "Unlimited Notes (Free limit: 5 notes)",
                description = "Free includes up to 5 notes and 5 sketches. Unlock unlimited notes, all 10 papers, 6 pens, and PDF/MD/TXT export."
            ) {
                val draft = if (template != null) {
                    NoteRecord(
                        id = 0L,
                        title = template.title,
                        content = if (template.noteType == "CHECKLIST") {
                            ChecklistSerializer.toPlainSummary(template.checklistItems)
                        } else {
                            template.bodyContent
                        },
                        categoryTag = "Notes",
                        colorHex = template.colorHex,
                        noteType = template.noteType,
                        checklistJson = if (template.noteType == "CHECKLIST") {
                            ChecklistSerializer.toJson(template.checklistItems)
                        } else "",
                        tagsCsv = template.tagsCsv,
                        folderName = template.folderName
                    )
                } else if (forceChecklist) {
                    NoteRecord(
                        id = 0L,
                        title = "",
                        content = "",
                        colorHex = "#C6F27A",
                        noteType = "CHECKLIST",
                        checklistJson = ChecklistSerializer.toJson(
                            listOf(
                                ChecklistItemData(id = 1L, text = "First item", checked = false),
                                ChecklistItemData(id = 2L, text = "Second item", checked = false)
                            )
                        )
                    )
                } else {
                    null
                }
                scanViewModel.openFullScreenNoteEditor(draft)
            }
            return
        }

        val draft = if (template != null) {
            NoteRecord(
                id = 0L,
                title = template.title,
                content = if (template.noteType == "CHECKLIST") {
                    ChecklistSerializer.toPlainSummary(template.checklistItems)
                } else {
                    template.bodyContent
                },
                categoryTag = "Notes",
                colorHex = template.colorHex,
                noteType = template.noteType,
                checklistJson = if (template.noteType == "CHECKLIST") {
                    ChecklistSerializer.toJson(template.checklistItems)
                } else "",
                tagsCsv = template.tagsCsv,
                folderName = template.folderName
            )
        } else if (forceChecklist) {
            NoteRecord(
                id = 0L,
                title = "",
                content = "",
                colorHex = "#C6F27A",
                noteType = "CHECKLIST",
                checklistJson = ChecklistSerializer.toJson(
                    listOf(
                        ChecklistItemData(id = 1L, text = "First item", checked = false),
                        ChecklistItemData(id = 2L, text = "Second item", checked = false)
                    )
                )
            )
        } else {
            null
        }
        scanViewModel.openFullScreenNoteEditor(draft)
    }

    fun tryOpenExistingNote(note: NoteRecord) {
        if (note.isLocked) {
            authenticateNoteBiometrics(
                context = context,
                title = "Unlock Note",
                subtitle = "Authenticate to open \"${note.title}\"",
                onAuthenticated = {
                    if (note.isDrawing) {
                        scanViewModel.openDrawingPad(note)
                    } else {
                        scanViewModel.openFullScreenNoteEditor(note)
                    }
                },
                onMessage = { msg -> scanViewModel.showToast(msg) }
            )
        } else {
            if (note.isDrawing) {
                scanViewModel.openDrawingPad(note)
            } else {
                scanViewModel.openFullScreenNoteEditor(note)
            }
        }
    }

    fun tryCreateNewSketch() {
        if (!isNotesUnlocked && allSketchesCount >= 5) {
            requireNotesPro(
                title = "Unlimited Sketches (Free limit: 5 sketches)",
                description = "Free includes up to 5 sketches, 3 papers, and 3 pens. Unlock unlimited sketches, all 10 procedural papers, and all 6 pens."
            ) {
                scanViewModel.openDrawingPad(null)
            }
        } else {
            scanViewModel.openDrawingPad(null)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Segmented Control: Text | Checklists | Drawing
        DyneSegmentedControl(
            segments = listOf("Text", "Checklists", "Drawing"),
            selectedIndex = modeIndex,
            height = 44.dp,
            onSegmentSelected = { idx ->
                FeedbackHelper.tick(context)
                modeIndex = idx
            }
        )

        // 2. Search Input + Accent "+" Action Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = scanViewModel::setSearchQuery,
                placeholder = {
                    Text(
                        text = when (modeIndex) {
                            1 -> "Search checklists..."
                            2 -> "Search sketches..."
                            else -> "Search notes, tags, folders..."
                        },
                        style = type.body,
                        color = theme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search notes", tint = theme.accent)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = theme.textSecondary,
                            modifier = Modifier.pressScaleClickable {
                                scanViewModel.setSearchQuery("")
                            }
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = theme.elevated,
                    unfocusedContainerColor = theme.elevated,
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("notes_search_input")
            )

            // Accent "+" button that opens full-screen editor (or full-screen drawing pad in Drawing mode)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .shadow(10.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.accent)
                    .pressScaleClickable(
                        testTag = "notes_accent_plus_button",
                        onClick = {
                            FeedbackHelper.confirmClick(context)
                            when (modeIndex) {
                                0 -> tryCreateOrOpenNote(template = null, forceChecklist = false)
                                1 -> tryCreateOrOpenNote(template = null, forceChecklist = true)
                                else -> tryCreateNewSketch()
                            }
                        }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New note or sketch",
                    tint = BentoTextBlack,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // 3. Free Templates Bar: Shopping list, Meeting notes, Trip checklist, Daily plan
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "FREE TEMPLATES",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )
                Text(
                    text = if (isNotesUnlocked) {
                        "Unlimited notes & sketches"
                    } else {
                        "Free: $allNonDrawingCount/5 notes • $allSketchesCount/5 sketches"
                    },
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.accent
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FreeNoteTemplates.forEach { tpl ->
                    val tplColor = parseHexColor(tpl.colorHex, theme.accent)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.elevated)
                            .border(1.dp, tplColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .pressScaleClickable(
                                testTag = "note_template_${tpl.id}",
                                onClick = {
                                    FeedbackHelper.tick(context)
                                    modeIndex = if (tpl.noteType == "CHECKLIST") 1 else 0
                                    tryCreateOrOpenNote(template = tpl)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(tplColor)
                        )
                        Column {
                            Text(
                                text = tpl.title,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                            Text(
                                text = tpl.subtitle,
                                style = type.caption.copy(fontSize = 10.sp),
                                color = theme.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 4. Smart Folders & Tags Filter Strip (Premium feature with ProBadge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            availableFolders.forEach { folder ->
                val selected = selectedSmartFolder == folder
                val isProSmartFilter = folder != "All" && folder != "Pinned" && !isNotesUnlocked
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) theme.accent.copy(alpha = 0.2f) else theme.elevated)
                        .border(
                            1.dp,
                            if (selected) theme.accent else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .pressScaleClickable {
                            if (isProSmartFilter) {
                                requireNotesPro(
                                    title = "Smart Folders & Tags",
                                    description = "Organize and filter notes with Smart Folders (Locked, Reminders, Work, Personal) and custom tags."
                                ) {
                                    selectedSmartFolder = folder
                                }
                            } else {
                                selectedSmartFolder = folder
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (selected) theme.accent else theme.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = folder,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) theme.accent else theme.textPrimary
                    )
                    if (isProSmartFilter) {
                        ProBadge()
                    }
                }
            }

            availableTags.filter { it != "All" }.forEach { tag ->
                val selected = selectedTagChip == tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) theme.bentoSky.copy(alpha = 0.22f) else theme.elevated)
                        .border(
                            1.dp,
                            if (selected) theme.bentoSky else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .pressScaleClickable {
                            if (!isNotesUnlocked) {
                                requireNotesPro(
                                    title = "Tags & Smart Folders",
                                    description = "Filter and organize notes by custom #tags and smart folders."
                                ) {
                                    selectedTagChip = if (selected) "All" else tag
                                }
                            } else {
                                selectedTagChip = if (selected) "All" else tag
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "#$tag",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) theme.bentoSky else theme.textSecondary
                    )
                }
            }
        }

        // 5. Mode Content: Text | Checklists | Drawing
        when (modeIndex) {
            0 -> {
                TextNotesTwoColumnGridSection(
                    textNotes = textNotes,
                    onOpenNote = ::tryOpenExistingNote,
                    onCreateNewNote = { tryCreateOrOpenNote(template = null, forceChecklist = false) },
                    onTogglePin = scanViewModel::toggleNotePinned,
                    showLibraryJumpButton = showLibraryJumpButton,
                    onJumpToLibraryNotes = onJumpToLibraryNotes
                )
            }

            1 -> {
                ChecklistsSectionContent(
                    checklistNotes = checklistNotes,
                    scanViewModel = scanViewModel,
                    onOpenChecklistInEditor = ::tryOpenExistingNote,
                    onCreateNewChecklist = { tryCreateOrOpenNote(template = null, forceChecklist = true) }
                )
            }

            else -> {
                DrawingSketchesGridSection(
                    drawingNotes = drawingNotes,
                    isNotesUnlocked = isNotesUnlocked,
                    onOpenSketch = { note -> tryOpenExistingNote(note) },
                    onNewSketch = ::tryCreateNewSketch,
                    onShareImage = { note ->
                        scanViewModel.shareDrawingPng(context, note) { chooser ->
                            context.startActivity(chooser)
                        }
                    },
                    onExportPdf = { note ->
                        requireNotesPro(
                            title = "Export Sketch to PDF with Page Size Selector",
                            description = "Export any sketch or note to standard A4, Letter, Legal, A5, ID Card, or Receipt PDFs."
                        ) {
                            notePendingPdfExport = note
                        }
                    },
                    onTogglePin = scanViewModel::toggleNotePinned,
                    onDelete = { note -> scanViewModel.deleteNote(note.id) }
                )
            }
        }

        if (!showLibraryJumpButton) {
            com.example.ads.InlineBannerAdSlot(placementTag = "notes_list_end")
        }
    }

    // Page size selector sheet for PDF export from card
    if (notePendingPdfExport != null) {
        val targetNote = notePendingPdfExport!!
        GlassSheet(
            onDismissRequest = { notePendingPdfExport = null },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "EXPORT TO PDF • PAGE SIZE",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    PdfPageSize.entries.forEach { sizeOpt ->
                        val selected = selectedPdfPageSize == sizeOpt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) theme.accent.copy(alpha = 0.2f) else theme.surface)
                                .border(
                                    1.dp,
                                    if (selected) theme.accent else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(12.dp)
                                )
                                .pressScaleClickable { selectedPdfPageSize = sizeOpt }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
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
                    text = "Export PDF (${selectedPdfPageSize.label})",
                    icon = Icons.Default.PictureAsPdf,
                    accentFill = theme.accent,
                    onClick = {
                        notePendingPdfExport = null
                        scanViewModel.exportNotePdf(context, targetNote, selectedPdfPageSize) { chooser ->
                            context.startActivity(chooser)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * TEXT SEGMENT:
 * 2-column grid of note cards (color stripe, title, 3-line preview, pin, date)
 * + accent "+" button that opens the full-screen editor.
 */
@Composable
private fun TextNotesTwoColumnGridSection(
    textNotes: List<NoteRecord>,
    onOpenNote: (NoteRecord) -> Unit,
    onCreateNewNote: () -> Unit,
    onTogglePin: (NoteRecord) -> Unit,
    showLibraryJumpButton: Boolean,
    onJumpToLibraryNotes: (() -> Unit)?
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.US) }

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
                text = "${textNotes.size} ${if (textNotes.size == 1) "note" else "notes"}",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showLibraryJumpButton && onJumpToLibraryNotes != null) {
                    GlassButton(
                        text = "Open Vault",
                        onClick = onJumpToLibraryNotes
                    )
                }
                GlassButton(
                    text = "+ New Note",
                    icon = Icons.Default.Add,
                    accentFill = theme.accent,
                    onClick = onCreateNewNote,
                    testTag = "new_text_note_button"
                )
            }
        }

        if (textNotes.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.elevated)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "No text notes yet",
                    style = type.section.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
                Text(
                    text = "Tap the accent + button or choose a free template above to open the full-screen note editor.",
                    style = type.caption,
                    color = theme.textSecondary
                )
                GlassButton(
                    text = "+ Create First Note",
                    icon = Icons.Default.Add,
                    accentFill = theme.accent,
                    onClick = onCreateNewNote,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            val rows = remember(textNotes) { textNotes.chunked(2) }
            rows.forEach { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pair.forEach { note ->
                        val stripeColor = parseHexColor(note.colorHex, theme.accent)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(148.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(theme.elevated)
                                .border(
                                    width = if (note.isPinned) 1.5.dp else 1.dp,
                                    color = if (note.isPinned) stripeColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.09f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .pressScaleClickable(
                                    testTag = "note_grid_card_${note.id}",
                                    onClick = { onOpenNote(note) }
                                )
                        ) {
                            // Color Stripe on left edge
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight()
                                    .background(stripeColor)
                            )

                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    // Title + Pin icon
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = note.title.ifBlank { "Untitled Note" },
                                            style = type.body.copy(fontWeight = FontWeight.Bold),
                                            color = theme.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (note.isLocked) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Locked",
                                                    tint = ProGold,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = "Pin",
                                                tint = if (note.isPinned) stripeColor else theme.textSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .pressScaleClickable { onTogglePin(note) }
                                            )
                                        }
                                    }

                                    // 3-line preview
                                    Text(
                                        text = if (note.isLocked) "Locked with biometrics • Tap to unlock" else note.content.ifBlank { "No additional text" },
                                        style = type.caption,
                                        color = if (note.isLocked) theme.textSecondary else theme.textPrimary.copy(alpha = 0.82f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Bottom row: Date + optional tag/reminder indicator
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = dateFmt.format(Date(note.timestamp)),
                                        style = type.caption.copy(fontSize = 10.sp),
                                        color = theme.textSecondary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (note.reminderEpochMs > 0L) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = "Reminder set",
                                                tint = stripeColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        if (note.tagsList.isNotEmpty()) {
                                            Text(
                                                text = "#${note.tagsList.first()}",
                                                style = type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = stripeColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * CHECKLISTS SEGMENT:
 * Items with check toggle, drag to reorder, and live circular progress ring.
 */
@Composable
private fun ChecklistsSectionContent(
    checklistNotes: List<NoteRecord>,
    scanViewModel: ScanViewModel,
    onOpenChecklistInEditor: (NoteRecord) -> Unit,
    onCreateNewChecklist: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

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
                text = "${checklistNotes.size} ${if (checklistNotes.size == 1) "checklist" else "checklists"}",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )
            GlassButton(
                text = "+ New Checklist",
                icon = Icons.Default.Checklist,
                accentFill = theme.accent,
                onClick = onCreateNewChecklist,
                testTag = "new_checklist_button"
            )
        }

        if (checklistNotes.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.elevated)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                ChecklistProgressRing(
                    completed = 0,
                    total = 4,
                    accentColor = theme.accent,
                    size = 56.dp
                )
                Text(
                    text = "No checklists yet",
                    style = type.section.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
                Text(
                    text = "Create an interactive checklist with check toggles, drag-to-reorder items, and a live progress ring.",
                    style = type.caption,
                    color = theme.textSecondary
                )
                GlassButton(
                    text = "+ Create Checklist",
                    icon = Icons.Default.Add,
                    accentFill = theme.accent,
                    onClick = onCreateNewChecklist,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            checklistNotes.forEach { checklistNote ->
                val accentColor = parseHexColor(checklistNote.colorHex, theme.accent)
                val itemsState = remember(checklistNote.id, checklistNote.checklistJson, checklistNote.content) {
                    mutableStateListOf<ChecklistItemData>().apply {
                        addAll(ChecklistSerializer.fromJson(checklistNote.checklistJson, checklistNote.content))
                    }
                }
                var quickAddItemText by rememberSaveable(checklistNote.id) { mutableStateOf("") }
                var draggedIndex by remember { mutableStateOf<Int?>(null) }
                var dragAccumulatedDy by remember { mutableFloatStateOf(0f) }

                val completedCount = itemsState.count { it.checked }
                val totalCount = itemsState.size

                fun persistChecklistChanges() {
                    scanViewModel.saveNote(
                        title = checklistNote.title,
                        content = ChecklistSerializer.toPlainSummary(itemsState),
                        categoryTag = checklistNote.categoryTag,
                        colorHex = checklistNote.colorHex,
                        isPinned = checklistNote.isPinned,
                        existingId = checklistNote.id,
                        noteType = "CHECKLIST",
                        checklistJson = ChecklistSerializer.toJson(itemsState),
                        tagsCsv = checklistNote.tagsCsv,
                        folderName = checklistNote.folderName,
                        isLocked = checklistNote.isLocked,
                        reminderEpochMs = checklistNote.reminderEpochMs,
                        attachedImagePathsJoined = checklistNote.attachedImagePathsJoined,
                        silentAutoSave = true
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(theme.elevated)
                        .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    // Header with Progress Ring + Title + Edit / Delete
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ChecklistProgressRing(
                            completed = completedCount,
                            total = totalCount,
                            accentColor = accentColor,
                            size = 46.dp
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .pressScaleClickable { onOpenChecklistInEditor(checklistNote) }
                        ) {
                            Text(
                                text = checklistNote.title.ifBlank { "Checklist" },
                                style = type.section.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$completedCount of $totalCount completed • Drag handle to reorder",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Open full editor",
                            tint = accentColor,
                            modifier = Modifier
                                .size(18.dp)
                                .pressScaleClickable { onOpenChecklistInEditor(checklistNote) }
                        )

                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete checklist",
                            tint = theme.bentoCoral,
                            modifier = Modifier
                                .size(18.dp)
                                .pressScaleClickable { scanViewModel.deleteNote(checklistNote.id) }
                        )
                    }

                    // Checklist items with check toggle + drag to reorder
                    itemsState.forEachIndexed { index, item ->
                        val isBeingDragged = draggedIndex == index
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isBeingDragged) accentColor.copy(alpha = 0.18f)
                                    else theme.surface
                                )
                                .border(
                                    1.dp,
                                    if (isBeingDragged) accentColor else Color.White.copy(alpha = 0.06f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            // Drag handle (drag vertically to reorder, plus up/down tap fallback)
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Drag to reorder",
                                tint = if (isBeingDragged) accentColor else theme.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .pointerInput(itemsState.size, index) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedIndex = index
                                                dragAccumulatedDy = 0f
                                                FeedbackHelper.tick(context)
                                            },
                                            onDragEnd = {
                                                draggedIndex = null
                                                dragAccumulatedDy = 0f
                                                persistChecklistChanges()
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                dragAccumulatedDy = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAccumulatedDy += dragAmount.y
                                                val thresholdPx = 38.dp.toPx()
                                                val currentIdx = draggedIndex ?: index
                                                if (dragAccumulatedDy > thresholdPx && currentIdx < itemsState.lastIndex) {
                                                    val moved = itemsState.removeAt(currentIdx)
                                                    itemsState.add(currentIdx + 1, moved)
                                                    draggedIndex = currentIdx + 1
                                                    dragAccumulatedDy = 0f
                                                    FeedbackHelper.tick(context)
                                                } else if (dragAccumulatedDy < -thresholdPx && currentIdx > 0) {
                                                    val moved = itemsState.removeAt(currentIdx)
                                                    itemsState.add(currentIdx - 1, moved)
                                                    draggedIndex = currentIdx - 1
                                                    dragAccumulatedDy = 0f
                                                    FeedbackHelper.tick(context)
                                                }
                                            }
                                        )
                                    }
                            )

                            // Check toggle
                            Icon(
                                imageVector = if (item.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Toggle check",
                                tint = if (item.checked) accentColor else theme.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .pressScaleClickable {
                                        FeedbackHelper.tick(context)
                                        itemsState[index] = item.copy(checked = !item.checked)
                                        persistChecklistChanges()
                                    }
                            )

                            Text(
                                text = item.text,
                                style = type.body.copy(
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (item.checked) theme.textSecondary else theme.textPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScaleClickable {
                                        FeedbackHelper.tick(context)
                                        itemsState[index] = item.copy(checked = !item.checked)
                                        persistChecklistChanges()
                                    }
                            )

                            // Accessible move up / down buttons alongside drag handle
                            if (index > 0) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    tint = theme.textSecondary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .pressScaleClickable {
                                            val moved = itemsState.removeAt(index)
                                            itemsState.add(index - 1, moved)
                                            persistChecklistChanges()
                                        }
                                )
                            }
                            if (index < itemsState.lastIndex) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    tint = theme.textSecondary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .pressScaleClickable {
                                            val moved = itemsState.removeAt(index)
                                            itemsState.add(index + 1, moved)
                                            persistChecklistChanges()
                                        }
                                )
                            }
                        }
                    }

                    // Quick add item row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = quickAddItemText,
                            onValueChange = { quickAddItemText = it },
                            placeholder = { Text("Add checklist item...", style = type.caption) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        GlassButton(
                            text = "Add",
                            icon = Icons.Default.Add,
                            accentFill = accentColor,
                            onClick = {
                                if (quickAddItemText.isNotBlank()) {
                                    itemsState.add(
                                        ChecklistItemData(
                                            id = System.nanoTime(),
                                            text = quickAddItemText.trim(),
                                            checked = false
                                        )
                                    )
                                    quickAddItemText = ""
                                    persistChecklistChanges()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Circular Progress Ring for Checklists.
 */
@Composable
fun ChecklistProgressRing(
    completed: Int,
    total: Int,
    accentColor: Color,
    size: Dp = 46.dp
) {
    val type = LocalDyneType.current
    val theme = LocalDyneTheme.current
    val progress = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val pct = (progress * 100).roundToInt()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 4.5.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$pct%",
            style = type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = theme.textPrimary
        )
    }
}

/**
 * DRAWING SEGMENT:
 * A grid of sketch thumbnails and "+ New sketch" button that opens the Full-Screen Drawing Pad route.
 */
@Composable
private fun DrawingSketchesGridSection(
    drawingNotes: List<NoteRecord>,
    isNotesUnlocked: Boolean,
    onOpenSketch: (NoteRecord) -> Unit,
    onNewSketch: () -> Unit,
    onShareImage: (NoteRecord) -> Unit,
    onExportPdf: (NoteRecord) -> Unit,
    onTogglePin: (NoteRecord) -> Unit,
    onDelete: (NoteRecord) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

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
                text = "${drawingNotes.size} ${if (drawingNotes.size == 1) "sketch" else "sketches"}",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )
            GlassButton(
                text = "+ New sketch",
                icon = Icons.Default.Brush,
                accentFill = theme.accent,
                onClick = onNewSketch,
                testTag = "new_sketch_button"
            )
        }

        if (drawingNotes.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.elevated)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Full-Screen Drawing Pad",
                    style = type.section.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
                Text(
                    text = "6 realistic pens (Ballpoint, Fountain, Marker, Pencil, Chalk, Highlighter) on 10 procedural paper surfaces with 60fps Bezier strokes.",
                    style = type.caption,
                    color = theme.textSecondary
                )
                GlassButton(
                    text = "+ New sketch",
                    icon = Icons.Default.Add,
                    accentFill = theme.accent,
                    onClick = onNewSketch,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "empty_state_new_sketch_button"
                )
            }
        } else {
            val rows = remember(drawingNotes) { drawingNotes.chunked(2) }
            rows.forEach { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pair.forEach { sketch ->
                        SketchThumbnailCard(
                            note = sketch,
                            isNotesUnlocked = isNotesUnlocked,
                            onOpen = { onOpenSketch(sketch) },
                            onShareImage = { onShareImage(sketch) },
                            onExportPdf = { onExportPdf(sketch) },
                            onTogglePin = { onTogglePin(sketch) },
                            onDelete = { onDelete(sketch) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SketchThumbnailCard(
    note: NoteRecord,
    isNotesUnlocked: Boolean,
    onOpen: () -> Unit,
    onShareImage: () -> Unit,
    onExportPdf: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val paper = remember(note.paperStyleId) { PaperStyle.fromId(note.paperStyleId) }

    val bitmap = remember(note.drawingPngPath) {
        if (note.drawingPngPath.isNotBlank() && File(note.drawingPngPath).exists()) {
            BitmapFactory.decodeFile(note.drawingPngPath)
        } else {
            null
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated)
            .border(
                width = if (note.isPinned) 1.5.dp else 1.dp,
                color = if (note.isPinned) theme.accent else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(22.dp)
            )
            .pressScaleClickable(
                testTag = "sketch_thumbnail_card_${note.id}",
                onClick = onOpen
            )
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(14.dp))
                .background(paper.baseComposeColor)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = note.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xB30F172A))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = paper.label,
                    style = type.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = note.title,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "Pin sketch",
                tint = if (note.isPinned) theme.accent else theme.textSecondary,
                modifier = Modifier
                    .size(16.dp)
                    .pressScaleClickable(onClick = onTogglePin)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Free: Share as image
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.surface)
                    .pressScaleClickable(onClick = onShareImage)
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share as image",
                    tint = theme.accent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Share",
                    style = type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
            }

            // Pro: PDF Export with Page Size Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.surface)
                    .pressScaleClickable(onClick = onExportPdf)
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Export PDF",
                    tint = if (isNotesUnlocked) theme.bentoSky else ProGold,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "PDF",
                    style = type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = if (isNotesUnlocked) theme.bentoSky else ProGold
                )
            }

            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete sketch",
                tint = theme.bentoCoral,
                modifier = Modifier
                    .size(17.dp)
                    .pressScaleClickable(onClick = onDelete)
            )
        }
    }
}

/**
 * FULL-SCREEN NOTE EDITOR (new route, hides the floating tab bar):
 * - Title, Body (or interactive Checklist with drag-to-reorder & progress ring), Color, Pin, Tags,
 *   Smart Folder, Auto-save, Share, Delete
 * - Pro features with FeatureGate & ProBadge:
 *   - Export to PDF with Page size selector
 *   - Markdown (.md) and TXT (.txt) export
 *   - Note lock with biometrics
 *   - Reminders
 *   - Tags & Smart Folders
 *   - Insert a photo (zero-permission Photo Picker) or scan from Vault into a note
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullScreenNoteEditorScreen(
    existingNote: NoteRecord?,
    scanViewModel: ScanViewModel,
    isPro: Boolean,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
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

    val allDocuments by scanViewModel.allDocuments.collectAsStateWithLifecycle()

    var noteId by rememberSaveable { mutableLongStateOf(existingNote?.id ?: 0L) }
    var noteTitle by rememberSaveable { mutableStateOf(existingNote?.title ?: "") }
    var noteBody by rememberSaveable { mutableStateOf(existingNote?.content ?: "") }
    var noteType by rememberSaveable { mutableStateOf(existingNote?.noteType ?: "TEXT") }
    var selectedColorHex by rememberSaveable {
        mutableStateOf(existingNote?.colorHex ?: NotePaletteColors.first().hex)
    }
    var isPinned by rememberSaveable { mutableStateOf(existingNote?.isPinned ?: false) }
    var isLocked by rememberSaveable { mutableStateOf(existingNote?.isLocked ?: false) }
    var tagsInput by rememberSaveable { mutableStateOf(existingNote?.tagsCsv ?: "") }
    var folderInput by rememberSaveable { mutableStateOf(existingNote?.folderName ?: "") }
    var reminderEpochMs by rememberSaveable { mutableLongStateOf(existingNote?.reminderEpochMs ?: 0L) }
    var attachedImagesJoined by rememberSaveable {
        mutableStateOf(existingNote?.attachedImagePathsJoined ?: "")
    }

    val checklistItems = remember(existingNote?.id) {
        mutableStateListOf<ChecklistItemData>().apply {
            if (existingNote?.noteType == "CHECKLIST") {
                addAll(ChecklistSerializer.fromJson(existingNote.checklistJson, existingNote.content))
            }
        }
    }
    var newChecklistItemText by rememberSaveable { mutableStateOf("") }

    var showExportSheet by remember { mutableStateOf(false) }
    var showInsertScanPickerSheet by remember { mutableStateOf(false) }
    var selectedPdfPageSize by remember { mutableStateOf(PdfPageSize.defaultForLocale()) }

    val activeColor = remember(selectedColorHex) {
        parseHexColor(selectedColorHex, theme.accent)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scanViewModel.attachImageUriToNote(context, uri) { savedPath ->
                val current = attachedImagesJoined.split("|").filter { it.isNotBlank() }
                attachedImagesJoined = (current + savedPath).joinToString("|")
            }
        }
    }

    fun buildCurrentRecordSnapshot(): NoteRecord {
        val resolvedBody = if (noteType == "CHECKLIST") {
            ChecklistSerializer.toPlainSummary(checklistItems)
        } else {
            noteBody
        }
        val resolvedChecklistJson = if (noteType == "CHECKLIST") {
            ChecklistSerializer.toJson(checklistItems)
        } else ""
        return NoteRecord(
            id = noteId,
            title = noteTitle.ifBlank { if (noteType == "CHECKLIST") "Checklist" else "Field Note" },
            content = resolvedBody,
            categoryTag = "Notes",
            colorHex = selectedColorHex,
            isPinned = isPinned,
            noteType = noteType,
            checklistJson = resolvedChecklistJson,
            tagsCsv = tagsInput.trim(),
            folderName = folderInput.trim(),
            isLocked = isLocked,
            reminderEpochMs = reminderEpochMs,
            attachedImagePathsJoined = attachedImagesJoined
        )
    }

    fun performAutoSaveAndClose() {
        val hasContent = noteTitle.isNotBlank() ||
            noteBody.isNotBlank() ||
            checklistItems.isNotEmpty() ||
            attachedImagesJoined.isNotBlank()
        if (!hasContent && noteId == 0L) {
            onClose()
            return
        }
        val snap = buildCurrentRecordSnapshot()
        scanViewModel.saveNote(
            title = snap.title,
            content = snap.content,
            categoryTag = snap.categoryTag,
            colorHex = snap.colorHex,
            isPinned = snap.isPinned,
            existingId = noteId,
            noteType = snap.noteType,
            checklistJson = snap.checklistJson,
            tagsCsv = snap.tagsCsv,
            folderName = snap.folderName,
            isLocked = snap.isLocked,
            reminderEpochMs = snap.reminderEpochMs,
            attachedImagePathsJoined = snap.attachedImagePathsJoined,
            silentAutoSave = false
        ) { saved ->
            noteId = saved.id
            onClose()
        }
    }

    BackHandler {
        performAutoSaveAndClose()
    }

    val attachedImagePaths = remember(attachedImagesJoined) {
        attachedImagesJoined.split("|").map { it.trim() }.filter { it.isNotBlank() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("full_screen_note_editor")
    ) {
        // Top Bar: Back with Auto-Save, Pin, Lock (Pro), Share, Export (Pro), Delete
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.elevated)
                        .border(1.dp, activeColor.copy(alpha = 0.5f), CircleShape)
                        .pressScaleClickable(
                            testTag = "note_editor_back_button",
                            onClick = {
                                FeedbackHelper.tick(context)
                                performAutoSaveAndClose()
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back and auto-save",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (noteType == "CHECKLIST") "CHECKLIST EDITOR" else "FULL-SCREEN NOTE",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = activeColor
                    )
                    Text(
                        text = "Auto-save enabled",
                        style = type.caption.copy(fontSize = 10.sp),
                        color = theme.textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pin toggle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPinned) activeColor.copy(alpha = 0.22f) else theme.elevated)
                        .border(1.dp, if (isPinned) activeColor else Color.White.copy(alpha = 0.1f), CircleShape)
                        .pressScaleClickable(
                            testTag = "note_editor_pin_button",
                            onClick = {
                                FeedbackHelper.tick(context)
                                isPinned = !isPinned
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin note",
                        tint = if (isPinned) activeColor else theme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Biometric Lock (Pro)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isLocked) ProGold.copy(alpha = 0.22f) else theme.elevated)
                        .border(1.dp, if (isLocked) ProGold else Color.White.copy(alpha = 0.1f), CircleShape)
                        .pressScaleClickable(
                            testTag = "note_editor_lock_button",
                            onClick = {
                                requireNotesPro(
                                    title = "Note Lock with Biometrics",
                                    description = "Protect sensitive notes and checklists with fingerprint, face, or device PIN authentication."
                                ) {
                                    authenticateNoteBiometrics(
                                        context = context,
                                        title = if (isLocked) "Unlock Note" else "Lock Note",
                                        subtitle = "Use biometrics or device credential",
                                        onAuthenticated = {
                                            isLocked = !isLocked
                                            scanViewModel.showToast(
                                                if (isLocked) "Note locked with biometrics" else "Note unlocked"
                                            )
                                        },
                                        onMessage = { msg -> scanViewModel.showToast(msg) }
                                    )
                                }
                            }
                        )
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock note",
                        tint = if (isLocked) ProGold else theme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Share plain text
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.elevated)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .pressScaleClickable(
                            testTag = "note_editor_share_button",
                            onClick = {
                                val snap = buildCurrentRecordSnapshot()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    this.type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, snap.title)
                                    putExtra(Intent.EXTRA_TEXT, "${snap.title}\n\n${snap.content}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share note",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Export PDF / MD / TXT
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.elevated)
                        .border(1.dp, activeColor.copy(alpha = 0.5f), CircleShape)
                        .pressScaleClickable(
                            testTag = "note_editor_export_button",
                            onClick = { showExportSheet = true }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = "Export note",
                        tint = activeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete
                if (noteId != 0L) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(theme.elevated)
                            .border(1.dp, theme.bentoCoral.copy(alpha = 0.4f), CircleShape)
                            .pressScaleClickable(
                                testTag = "note_editor_delete_button",
                                onClick = {
                                    scanViewModel.deleteNote(noteId)
                                    onClose()
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete note",
                            tint = theme.bentoCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Editor Scrollable Body
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Color Stripe Picker (12 colors)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                NotePaletteColors.forEach { option ->
                    val selected = selectedColorHex.equals(option.hex, ignoreCase = true)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) Color.White else Color.Black.copy(alpha = 0.3f),
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
                                tint = BentoTextBlack,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Note Title Input
            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                placeholder = { Text("Note title...", style = type.section, color = theme.textSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeColor,
                    unfocusedBorderColor = activeColor.copy(alpha = 0.35f),
                    focusedContainerColor = theme.elevated,
                    unfocusedContainerColor = theme.elevated,
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            // Mode switch between Text Body and Interactive Checklist
            if (noteType == "CHECKLIST") {
                val doneCount = checklistItems.count { it.checked }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.elevated)
                        .padding(12.dp)
                ) {
                    ChecklistProgressRing(
                        completed = doneCount,
                        total = checklistItems.size,
                        accentColor = activeColor,
                        size = 48.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$doneCount of ${checklistItems.size} items completed",
                            style = type.body.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "Tap checkmarks or use arrows to reorder items",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                }

                checklistItems.forEachIndexed { idx, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.elevated)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (item.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Check",
                            tint = if (item.checked) activeColor else theme.textSecondary,
                            modifier = Modifier
                                .size(22.dp)
                                .pressScaleClickable {
                                    checklistItems[idx] = item.copy(checked = !item.checked)
                                }
                        )
                        Text(
                            text = item.text,
                            style = type.body.copy(
                                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (item.checked) theme.textSecondary else theme.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (idx > 0) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move up",
                                tint = theme.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .pressScaleClickable {
                                        val moved = checklistItems.removeAt(idx)
                                        checklistItems.add(idx - 1, moved)
                                    }
                            )
                        }
                        if (idx < checklistItems.lastIndex) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move down",
                                tint = theme.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .pressScaleClickable {
                                        val moved = checklistItems.removeAt(idx)
                                        checklistItems.add(idx + 1, moved)
                                    }
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove item",
                            tint = theme.bentoCoral,
                            modifier = Modifier
                                .size(18.dp)
                                .pressScaleClickable { checklistItems.removeAt(idx) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newChecklistItemText,
                        onValueChange = { newChecklistItemText = it },
                        placeholder = { Text("Add checklist item...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Add",
                        icon = Icons.Default.Add,
                        accentFill = activeColor,
                        onClick = {
                            if (newChecklistItemText.isNotBlank()) {
                                checklistItems.add(
                                    ChecklistItemData(
                                        id = System.nanoTime(),
                                        text = newChecklistItemText.trim(),
                                        checked = false
                                    )
                                )
                                newChecklistItemText = ""
                            }
                        }
                    )
                }
            } else {
                OutlinedTextField(
                    value = noteBody,
                    onValueChange = { noteBody = it },
                    placeholder = {
                        Text(
                            "Write notes, observations, meeting points, or calculations...",
                            color = theme.textSecondary
                        )
                    },
                    minLines = 8,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = theme.elevated,
                        unfocusedContainerColor = theme.elevated,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_body_input")
                )
            }

            // Attached Photos & Scans Strip
            if (attachedImagePaths.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ATTACHED PHOTOS & SCANS (${attachedImagePaths.size})",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        attachedImagePaths.forEach { path ->
                            val bmp = remember(path) {
                                if (File(path).exists()) BitmapFactory.decodeFile(path) else null
                            }
                            Box(
                                contentAlignment = Alignment.TopEnd,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(theme.elevated)
                                    .border(1.dp, activeColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Attached photo or scan",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xCC0F172A))
                                        .pressScaleClickable {
                                            attachedImagesJoined = attachedImagePaths
                                                .filterNot { it == path }
                                                .joinToString("|")
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove attachment",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pro Tools Row: Insert Photo, Insert Scan, Reminder, Tags & Smart Folder
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.elevated)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ATTACHMENTS, TAGS, FOLDERS & REMINDERS",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    if (!isNotesUnlocked) {
                        ProBadge()
                    }
                }

                // Insert Photo or Scan into Note (Premium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Insert Photo",
                        icon = Icons.Default.AddPhotoAlternate,
                        onClick = {
                            requireNotesPro(
                                title = "Insert Photo or Scan into Note",
                                description = "Attach photos from your gallery or scanned documents from your Library directly into any note."
                            ) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "note_insert_photo_button"
                    )

                    GlassButton(
                        text = "Insert Scan",
                        icon = Icons.Default.DocumentScanner,
                        onClick = {
                            requireNotesPro(
                                title = "Insert Photo or Scan into Note",
                                description = "Attach scanned documents from your Library directly into any note."
                            ) {
                                showInsertScanPickerSheet = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "note_insert_scan_button"
                    )
                }

                // Tags & Smart Folder Inputs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { newVal ->
                            if (!isNotesUnlocked && tagsInput.isEmpty() && newVal.isNotEmpty()) {
                                requireNotesPro(
                                    title = "Tags & Smart Folders",
                                    description = "Organize notes with custom #tags and smart folders."
                                ) {
                                    tagsInput = newVal
                                }
                            } else {
                                tagsInput = newVal
                            }
                        },
                        label = { Text("Tags (comma separated)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = folderInput,
                        onValueChange = { newVal ->
                            if (!isNotesUnlocked && folderInput.isEmpty() && newVal.isNotEmpty()) {
                                requireNotesPro(
                                    title = "Tags & Smart Folders",
                                    description = "Organize notes into custom Smart Folders."
                                ) {
                                    folderInput = newVal
                                }
                            } else {
                                folderInput = newVal
                            }
                        },
                        label = { Text("Folder") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Reminder presets row (Pro)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Reminder",
                        tint = if (reminderEpochMs > 0L) activeColor else theme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    val reminderPresets = listOf(
                        "In 1 hour" to 3_600_000L,
                        "Tonight (4h)" to 14_400_000L,
                        "Tomorrow" to 86_400_000L
                    )
                    reminderPresets.forEach { (label, deltaMs) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.surface)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .pressScaleClickable {
                                    requireNotesPro(
                                        title = "Note Reminders",
                                        description = "Schedule time-based reminders for important notes and checklists."
                                    ) {
                                        reminderEpochMs = System.currentTimeMillis() + deltaMs
                                        scanViewModel.showToast("Reminder set: $label")
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                        }
                    }
                    if (reminderEpochMs > 0L) {
                        val remStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(reminderEpochMs))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(activeColor.copy(alpha = 0.2f))
                                .border(1.dp, activeColor, RoundedCornerShape(12.dp))
                                .pressScaleClickable { reminderEpochMs = 0L }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Active: $remStr ✕",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = activeColor
                            )
                        }
                    }
                }
            }

            // Save & Done Button
            GlassButton(
                text = "Save & Close Note",
                icon = Icons.Default.Check,
                accentFill = activeColor,
                onClick = { performAutoSaveAndClose() },
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_text_note_button"
            )
        }
    }

    // Insert Scan from Vault Sheet
    if (showInsertScanPickerSheet) {
        GlassSheet(
            onDismissRequest = { showInsertScanPickerSheet = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "INSERT SCANNED DOCUMENT INTO NOTE",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
                if (allDocuments.isEmpty()) {
                    Text(
                        text = "No scanned documents in your Vault yet. Scan a document first or use Insert Photo.",
                        style = type.body,
                        color = theme.textSecondary
                    )
                } else {
                    allDocuments.take(8).forEach { doc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(theme.surface)
                                .pressScaleClickable {
                                    val thumb = doc.thumbnailPath.ifBlank { doc.pagePaths.firstOrNull().orEmpty() }
                                    if (thumb.isNotBlank()) {
                                        val current = attachedImagesJoined.split("|").filter { it.isNotBlank() }
                                        attachedImagesJoined = (current + thumb).joinToString("|")
                                        scanViewModel.showToast("Inserted scan \"${doc.name}\"")
                                    }
                                    showInsertScanPickerSheet = false
                                }
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.name,
                                    style = type.body.copy(fontWeight = FontWeight.Bold),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "${doc.pageCount} page(s) • ${doc.category}",
                                    style = type.caption,
                                    color = theme.textSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Insert",
                                tint = theme.accent
                            )
                        }
                    }
                }
            }
        }
    }

    // Export Note Sheet: PDF with Page Size Selector, Markdown (.md), TXT (.txt)
    if (showExportSheet) {
        GlassSheet(
            onDismissRequest = { showExportSheet = false },
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
                        text = "EXPORT NOTE (PDF / MARKDOWN / TXT)",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                    if (!isNotesUnlocked) {
                        ProBadge()
                    }
                }

                Text(
                    text = "PDF Page Size:",
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    PdfPageSize.entries.forEach { sizeOpt ->
                        val selected = selectedPdfPageSize == sizeOpt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) theme.accent.copy(alpha = 0.2f) else theme.surface)
                                .border(
                                    1.dp,
                                    if (selected) theme.accent else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(12.dp)
                                )
                                .pressScaleClickable { selectedPdfPageSize = sizeOpt }
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
                    text = "Export to PDF (${selectedPdfPageSize.label})",
                    icon = Icons.Default.PictureAsPdf,
                    accentFill = if (isNotesUnlocked) theme.accent else ProGold,
                    onClick = {
                        requireNotesPro(
                            title = "Export to PDF with Page Size Selector",
                            description = "Export notes and checklists to PDF with selectable page sizes (Auto, A4, Letter, Legal, A5, ID Card, Receipt)."
                        ) {
                            val snap = buildCurrentRecordSnapshot()
                            showExportSheet = false
                            scanViewModel.exportNotePdf(context, snap, selectedPdfPageSize) { chooser ->
                                context.startActivity(chooser)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Export Markdown (.md)",
                        icon = Icons.Default.Description,
                        onClick = {
                            requireNotesPro(
                                title = "Markdown (.md) & TXT Export",
                                description = "Export formatted notes and checklists as standard Markdown (.md) or plain text (.txt) files."
                            ) {
                                val snap = buildCurrentRecordSnapshot()
                                showExportSheet = false
                                scanViewModel.exportNoteMarkdownOrTxt(context, snap, asMarkdown = true) { chooser ->
                                    context.startActivity(chooser)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    GlassButton(
                        text = "Export TXT (.txt)",
                        icon = Icons.Default.Download,
                        onClick = {
                            requireNotesPro(
                                title = "Markdown (.md) & TXT Export",
                                description = "Export formatted notes and checklists as standard Markdown (.md) or plain text (.txt) files."
                            ) {
                                val snap = buildCurrentRecordSnapshot()
                                showExportSheet = false
                                scanViewModel.exportNoteMarkdownOrTxt(context, snap, asMarkdown = false) { chooser ->
                                    context.startActivity(chooser)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
