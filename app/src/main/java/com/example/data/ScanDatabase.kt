package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class ScanContentType(val displayLabel: String) {
    URL("Website URL"),
    WIFI("Wi-Fi Network"),
    PHONE("Phone Number"),
    EMAIL("Email Address"),
    SMS("SMS Message"),
    CONTACT("Contact Card"),
    LOCATION("Geo Coordinates"),
    PRODUCT("Product Barcode"),
    TEXT("Plain Text")
}

enum class DocumentCategory(val label: String) {
    ID("ID"),
    RECEIPT("Receipt"),
    BILL("Bill"),
    NOTES("Notes"),
    OTHER("Other")
}

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawContent: String,
    val displayTitle: String,
    val formatName: String,
    val is2dMatrix: Boolean,
    val contentType: String,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val parsedContentType: ScanContentType
        get() = runCatching { ScanContentType.valueOf(contentType) }.getOrDefault(ScanContentType.TEXT)
}

enum class PdfPageSize(
    val label: String,
    val subtitle: String,
    val widthPt: Int,
    val heightPt: Int
) {
    AUTO("Auto", "Keep scan aspect", 0, 0),
    A4("A4", "595×842 pt", 595, 842),
    US_LETTER("US Letter", "612×792 pt", 612, 792),
    LEGAL("Legal", "612×1008 pt", 612, 1008),
    A5("A5", "420×595 pt", 420, 595),
    A3("A3", "842×1191 pt", 842, 1191),
    ID_CARD("ID card", "243×153 pt", 243, 153),
    BUSINESS_CARD("Business card", "252×144 pt", 252, 144),
    RECEIPT("Receipt", "226 pt wide", 226, 0);

    val id: String
        get() = name

    fun resolvePageDimensionsPt(bitmapWidth: Int, bitmapHeight: Int): Pair<Int, Int> {
        val safeW = bitmapWidth.coerceAtLeast(1)
        val safeH = bitmapHeight.coerceAtLeast(1)
        val isLandscape = safeW > safeH
        return when (this) {
            AUTO -> {
                if (isLandscape) {
                    val w = 842
                    val h = ((842f * safeH) / safeW).toInt().coerceAtLeast(100)
                    w to h
                } else {
                    val w = 595
                    val h = ((595f * safeH) / safeW).toInt().coerceAtLeast(100)
                    w to h
                }
            }
            RECEIPT -> {
                val w = 226
                val h = ((226f * safeH) / safeW).toInt().coerceIn(140, 2800)
                w to h
            }
            else -> {
                val shortSide = minOf(widthPt, heightPt)
                val longSide = maxOf(widthPt, heightPt)
                if (isLandscape) {
                    longSide to shortSide
                } else {
                    shortSide to longSide
                }
            }
        }
    }

    companion object {
        fun defaultForLocale(locale: java.util.Locale = java.util.Locale.getDefault()): PdfPageSize {
            val country = locale.country.uppercase(java.util.Locale.US)
            return if (country == "US" || country == "CA") US_LETTER else A4
        }

        fun fromId(id: String?, locale: java.util.Locale = java.util.Locale.getDefault()): PdfPageSize {
            if (id.isNullOrBlank()) return defaultForLocale(locale)
            return entries.firstOrNull { it.name.equals(id.trim(), ignoreCase = true) }
                ?: defaultForLocale(locale)
        }

        /**
         * Compares the scan's aspect ratio (shortSide / longSide) with standard sizes (±3% tolerance)
         * and returns the closest matching standard PdfPageSize.
         */
        fun detectClosestStandardSize(bitmapWidth: Int, bitmapHeight: Int): PdfPageSize? {
            if (bitmapWidth <= 0 || bitmapHeight <= 0) return null
            val shortSide = minOf(bitmapWidth, bitmapHeight).toFloat()
            val longSide = maxOf(bitmapWidth, bitmapHeight).toFloat()
            if (longSide / shortSide >= 2.15f) {
                return RECEIPT
            }
            val scanRatio = shortSide / longSide
            // Standard normalized short/long ratios:
            // US_LETTER: 612/792 = 0.7727
            // A4: 595/842 = 0.7067
            // ID_CARD: 153/243 = 0.6296
            // LEGAL: 612/1008 = 0.6071
            // BUSINESS_CARD: 144/252 = 0.5714
            val candidates = listOf(
                A4 to (595f / 842f),
                US_LETTER to (612f / 792f),
                ID_CARD to (153f / 243f),
                LEGAL to (612f / 1008f),
                BUSINESS_CARD to (144f / 252f)
            )
            var bestMatch: PdfPageSize? = null
            var bestRelativeDiff = Float.MAX_VALUE
            for ((size, targetRatio) in candidates) {
                val relDiff = kotlin.math.abs(scanRatio - targetRatio) / targetRatio
                if (relDiff <= 0.03f && relDiff < bestRelativeDiff) {
                    bestRelativeDiff = relDiff
                    bestMatch = size
                }
            }
            return bestMatch
        }
    }
}

enum class PdfFitMode(val label: String) {
    FIT("Fit"),
    FILL("Fill");

    val id: String
        get() = name

    companion object {
        fun fromId(id: String?): PdfFitMode =
            entries.firstOrNull { it.name.equals(id?.trim(), ignoreCase = true) } ?: FIT
    }
}

enum class PdfMarginMode(val label: String, val mm: Int, val points: Float) {
    NONE("None", 0, 0f),
    SMALL_10MM("Small 10 mm", 10, 28.35f),
    NORMAL_20MM("Normal 20 mm", 20, 56.69f);

    val id: String
        get() = name

    val marginPt: Float
        get() = points

    companion object {
        fun fromId(id: String?): PdfMarginMode =
            entries.firstOrNull { it.name.equals(id?.trim(), ignoreCase = true) } ?: SMALL_10MM
    }
}

enum class PdfQualityDpi(val label: String, val dpi: Int, val jpegQuality: Int) {
    ORIGINAL("Original", 0, 95),
    HIGH_200("High (200 DPI)", 200, 88),
    MEDIUM_150("Medium (150 DPI)", 150, 78),
    LOW_100("Low (100 DPI)", 100, 65);

    val id: String
        get() = name

    companion object {
        val SMALL_100: PdfQualityDpi
            get() = LOW_100

        fun fromId(id: String?): PdfQualityDpi =
            entries.firstOrNull { it.name.equals(id?.trim(), ignoreCase = true) } ?: HIGH_200
    }
}

@Entity(tableName = "document_records")
data class DocumentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Other", // ID, Receipt, Bill, Notes, Other
    val createdAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 1,
    val pdfPath: String = "",
    val thumbnailPath: String = "",
    val pagePathsJoined: String = "",
    val originalPagePathsJoined: String = "",
    val ocrText: String = "",
    val favorite: Boolean = false,
    val locked: Boolean = false,
    val filterMode: String = "Crisp B&W",
    val pageSizeId: String = "",
    val fitModeId: String = "FIT",
    val marginModeId: String = "SMALL_10MM",
    val qualityDpiId: String = "HIGH_200",
    val folderName: String = "",
    val tagsCsv: String = "",
    val expiryEpochMs: Long = 0L,
    val merchantName: String = "",
    val receiptDateIso: String = "",
    val receiptTotalAmount: Double = 0.0,
    val expenseCategory: String = "",
    val isPasswordProtected: Boolean = false,
    val brightnessDelta: Float = 0f,
    val contrastScale: Float = 1f,
    val sharpenAmount: Float = 0f
) {
    val timestamp: Long
        get() = createdAt

    val tagsList: List<String>
        get() = tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }

    val pagePaths: List<String>
        get() = if (pagePathsJoined.isBlank()) {
            if (thumbnailPath.isNotBlank()) listOf(thumbnailPath) else emptyList()
        } else {
            pagePathsJoined.split("|").filter { it.isNotBlank() }
        }

    val originalPagePaths: List<String>
        get() = if (originalPagePathsJoined.isBlank()) {
            pagePaths
        } else {
            originalPagePathsJoined.split("|").filter { it.isNotBlank() }.ifEmpty { pagePaths }
        }

    val resolvedPageSize: PdfPageSize
        get() = PdfPageSize.fromId(pageSizeId)

    val resolvedFitMode: PdfFitMode
        get() = PdfFitMode.fromId(fitModeId)

    val resolvedMarginMode: PdfMarginMode
        get() = PdfMarginMode.fromId(marginModeId)

    val resolvedQualityDpi: PdfQualityDpi
        get() = PdfQualityDpi.fromId(qualityDpiId)
}

@Entity(tableName = "note_records")
data class NoteRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val categoryTag: String = "Note",
    val colorHex: String = "#C6F27A",
    val isPinned: Boolean = false,
    val isDrawing: Boolean = false,
    val drawingPngPath: String = "",
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val noteType: String = if (isDrawing) "DRAWING" else "TEXT", // TEXT, CHECKLIST, DRAWING
    val checklistJson: String = "",
    val tagsCsv: String = "",
    val folderName: String = "",
    val isLocked: Boolean = false,
    val reminderEpochMs: Long = 0L,
    val attachedImagePathsJoined: String = "",
    val paperStyleId: String = "PLAIN_WHITE",
    val strokesJson: String = "",
    val recognizedHandwritingText: String = ""
) {
    val tagsList: List<String>
        get() = tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }

    val attachedImagePaths: List<String>
        get() = attachedImagePathsJoined.split("|").map { it.trim() }.filter { it.isNotBlank() }
}

@Entity(tableName = "coin_ledger")
data class CoinLedgerRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val delta: Int,
    val balanceAfter: Int,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_recent_files")
data class MediaRecentFileRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val toolId: String,
    val toolName: String,
    val mimeType: String,
    val outputUriString: String = "",
    val localCachePath: String = "",
    val beforeSizeBytes: Long = 0L,
    val afterSizeBytes: Long = 0L,
    val summaryLabel: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ScanDao {
    // --- QR & Barcode Scans ---
    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllScansFlow(): Flow<List<ScanRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(record: ScanRecord): Long

    @Query("UPDATE scan_records SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setScanFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("DELETE FROM scan_records WHERE id NOT IN (SELECT id FROM scan_records ORDER BY timestamp DESC LIMIT :maxCount)")
    suspend fun trimHistoryToLimit(maxCount: Int)

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    suspend fun getAllScansSnapshot(): List<ScanRecord>

    // --- Documents ---
    @Query("SELECT * FROM document_records ORDER BY createdAt DESC")
    fun getAllDocumentsFlow(): Flow<List<DocumentRecord>>

    @Query("SELECT * FROM document_records WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(record: DocumentRecord): Long

    @Update
    suspend fun updateDocument(record: DocumentRecord)

    @Query("UPDATE document_records SET favorite = :favorite WHERE id = :id")
    suspend fun setDocumentFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE document_records SET locked = :locked WHERE id = :id")
    suspend fun setDocumentLocked(id: Long, locked: Boolean)

    @Query("UPDATE document_records SET name = :name WHERE id = :id")
    suspend fun renameDocument(id: Long, name: String)

    @Query("UPDATE document_records SET ocrText = :ocrText WHERE id = :id")
    suspend fun updateDocumentOcrText(id: Long, ocrText: String)

    @Query("DELETE FROM document_records WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    // --- Field Notes & Drawings (Pinned first, then newest) ---
    @Query("SELECT * FROM note_records ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotesFlow(): Flow<List<NoteRecord>>

    @Query("SELECT * FROM note_records WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(record: NoteRecord): Long

    @Update
    suspend fun updateNote(record: NoteRecord)

    @Query("UPDATE note_records SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setNoteFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE note_records SET isPinned = :isPinned WHERE id = :id")
    suspend fun setNotePinned(id: Long, isPinned: Boolean)

    @Query("UPDATE note_records SET isLocked = :isLocked WHERE id = :id")
    suspend fun setNoteLocked(id: Long, isLocked: Boolean)

    @Query("DELETE FROM note_records WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM scan_records")
    suspend fun deleteAllScans()

    @Query("DELETE FROM document_records")
    suspend fun deleteAllDocuments()

    @Query("DELETE FROM note_records")
    suspend fun deleteAllNotes()

    @Query("UPDATE document_records SET locked = :locked")
    suspend fun setAllDocumentsLocked(locked: Boolean)

    @Query("UPDATE note_records SET isLocked = :locked")
    suspend fun setAllNotesLocked(locked: Boolean)

    @Query("SELECT * FROM document_records ORDER BY createdAt DESC")
    suspend fun getAllDocumentsSnapshot(): List<DocumentRecord>

    @Query("SELECT * FROM note_records ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAllNotesSnapshot(): List<NoteRecord>

    // --- Coin Ledger ---
    @Query("SELECT * FROM coin_ledger ORDER BY timestamp DESC LIMIT 100")
    fun getAllCoinLedgerFlow(): Flow<List<CoinLedgerRecord>>

    @Query("SELECT * FROM coin_ledger ORDER BY timestamp DESC")
    suspend fun getAllCoinLedgerSnapshot(): List<CoinLedgerRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoinLedger(record: CoinLedgerRecord): Long

    @Query("DELETE FROM coin_ledger")
    suspend fun deleteAllCoinLedger()

    // --- Media Studio Recent Files ---
    @Query("SELECT * FROM media_recent_files ORDER BY timestamp DESC LIMIT 50")
    fun getAllMediaRecentFilesFlow(): Flow<List<MediaRecentFileRecord>>

    @Query("SELECT * FROM media_recent_files ORDER BY timestamp DESC")
    suspend fun getAllMediaRecentFilesSnapshot(): List<MediaRecentFileRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaRecentFile(record: MediaRecentFileRecord): Long

    @Query("DELETE FROM media_recent_files WHERE id = :id")
    suspend fun deleteMediaRecentFileById(id: Long)

    @Query("DELETE FROM media_recent_files")
    suspend fun deleteAllMediaRecentFiles()
}

@Database(
    entities = [
        ScanRecord::class,
        DocumentRecord::class,
        NoteRecord::class,
        CoinLedgerRecord::class,
        MediaRecentFileRecord::class
    ],
    version = 9,
    exportSchema = false
)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: ScanDatabase? = null

        fun getInstance(context: Context): ScanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScanDatabase::class.java,
                    "dyne_local_vault.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
