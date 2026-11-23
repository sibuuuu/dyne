package com.example.billing

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.PreferencesRepository
import com.example.data.ScanRepository
import com.example.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UnlockCategory(val title: String) {
    TOOLS("Tools"),
    OUTDOORS("Outdoors"),
    DOCUMENTS_AND_PDF("Documents & PDF"),
    MEDIA_STUDIO("Media Studio"),
    NOTES_AND_DRAWING("Notes & Drawing"),
    ALERTS_AND_WIDGETS("Alerts & Widgets"),
    THEMES_AND_CUSTOMIZATION("Themes & Customization"),
    BACKUP_AND_PRIVACY("Backup & Privacy"),
    NO_ADS("No ads");

    companion object {
        val DOCUMENTS_AND_NOTES: UnlockCategory get() = DOCUMENTS_AND_PDF
    }
}

/**
 * Single authoritative enum of all Premium features in Dyne.
 * Everything not in this enum (or SoundPack) is free.
 */
enum class PremiumFeature(
    val title: String,
    val description: String,
    val category: UnlockCategory,
    val coinCost: Int = 10,
    val toolId: String? = null
) {
    ANGLE(
        title = "Angle / Inclinometer",
        description = "Precision inclinometer protractor with Hold and Zero calibration.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "angle"
    ),
    METAL_DETECTOR(
        title = "Metal Detector",
        description = "Magnetic field µT circular gauge with sensitivity and audio-haptic alerts.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "metal_finder"
    ),
    SOUND_METER(
        title = "Sound Meter",
        description = "Live acoustic dB gauge, min/avg/max, reference levels, and 30s history waveform.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "sound_db"
    ),
    FLASHLIGHT_HIGH(
        title = "Flashlight Brightness",
        description = "Full LED torch intensity control from Dim to hardware MAX.",
        category = UnlockCategory.TOOLS,
        coinCost = 10
    ),
    SPEEDOMETER_HUD_TRIP(
        title = "Speedometer HUD & Trip Log",
        description = "Windshield HUD mirror mode plus GPS trip distance, duration, and average speed log.",
        category = UnlockCategory.TOOLS,
        coinCost = 10
    ),
    WALK_RUN_TRACKER(
        title = "Walk/Run Tracker",
        description = "GPS route drawn on Canvas, approximate distance, pace, splits, saved sessions & GPX/CSV export.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10,
        toolId = "walk_run_tracker"
    ),
    ALTIMETER(
        title = "Altimeter & Pressure Trend",
        description = "Approximate barometric elevation, sea-level QNH calibration, and pressure trend chart.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10,
        toolId = "altimeter"
    ),
    VIBRATION_GFORCE(
        title = "Vibration & G-Force Meter",
        description = "Approximate 3-axis accelerometer vibration & G-force meter with peak hold and waveform.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "vibration_gforce"
    ),
    FISHING_LOG(
        title = "Fishing Log",
        description = "Catch notes with time, GPS location, moon phase, weather/barometer & CSV export.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10,
        toolId = "fishing_log"
    ),
    SCIENTIFIC_DIY_CALCULATORS(
        title = "Scientific & DIY Calculators",
        description = "Scientific trigonometric keypad plus right-triangle pitch, roof, and ratio solver.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "tip_calc"
    ),
    INTERVAL_TIMER(
        title = "Interval Timer",
        description = "Customizable work/rest rounds with audio and haptic interval cues.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "interval_timer"
    ),
    TONE_GENERATOR_PRO(
        title = "Tone Generator Waveforms & Sweep",
        description = "Square, saw, triangle, frequency sweep, and dual-tone synthesis.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "tone_generator_pro"
    ),
    NOISE_SLEEP_MIXER(
        title = "Noise & Sleep Mixer",
        description = "Synthesized white, pink, brown, rain, and wind layers with fade-out sleep timer.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "noise_sleep_mixer"
    ),
    TUNER(
        title = "Chromatic & Instrument Tuner",
        description = "Microphone pitch detection by autocorrelation with needle gauge and cents readout.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "tuner"
    ),
    FREQUENCY_PRO(
        title = "Frequency Pro & Sound Packs",
        description = "20 Hz–20 kHz range, all waveforms, sweep/pulse/loop modes, 100% volume, presets & Sound Packs.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "frequency_pro"
    ),
    QR_GENERATOR_STYLES(
        title = "QR Generator Styles",
        description = "Custom QR code generator with color palettes, quiet zones, and module styles.",
        category = UnlockCategory.DOCUMENTS_AND_PDF,
        coinCost = 10,
        toolId = "qr_generator"
    ),
    SOLUNAR_7DAY(
        title = "7-Day Solunar Forecast",
        description = "Full 7-day major/minor feeding windows and daily Solunar activity scores.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10
    ),
    SUN_MOON_DOME(
        title = "Full Interactive Sky Dome",
        description = "Interactive 3D sky compass dome with draggable sun path, golden hour, and blue hour.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10
    ),
    WEATHER_HOURLY_7DAY(
        title = "Hourly & 7-Day Weather",
        description = "24-hour hourly strip and 7-day barometric Open-Meteo outdoor forecast.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10
    ),
    UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE(
        title = "Premium Document & PDF Pack",
        description = "Unlimited documents, PDF compress/password/watermark/stamps/crop/redact, Pro scan filters, ID card, Receipt & Expense, Business Card, OCR translate, folders & ZIP.",
        category = UnlockCategory.DOCUMENTS_AND_PDF,
        coinCost = 10
    ),
    UNLIMITED_NOTES_PENS_PAPERS(
        title = "Premium Notes & Drawing Pack",
        description = "All 10 papers & 6 pens, unlimited notes & sketches, PDF/MD/TXT export, biometric lock, reminders, tags & smart folders, handwriting to text, photo/scan insert, and shape recognition.",
        category = UnlockCategory.NOTES_AND_DRAWING,
        coinCost = 10
    ),
    CSV_EXPORT(
        title = "CSV / PDF Export",
        description = "Export scan history and field logs to standard CSV or PDF files.",
        category = UnlockCategory.DOCUMENTS_AND_PDF,
        coinCost = 10
    ),
    UNLIMITED_WORLD_CLOCKS(
        title = "Unlimited World Clocks",
        description = "Pin unlimited global cities and time zones with interactive meeting planner.",
        category = UnlockCategory.TOOLS,
        coinCost = 10
    ),
    PREMIUM_THEMES(
        title = "6 Extra Pro Themes",
        description = "Ocean, Sunset, Aurora, Sakura, AMOLED Mono, and Midnight Violet glass palettes.",
        category = UnlockCategory.THEMES_AND_CUSTOMIZATION,
        coinCost = 10
    ),
    NO_ADS(
        title = "No Ads",
        description = "Zero banner, native, interstitial, or app-open ads anywhere in Dyne.",
        category = UnlockCategory.NO_ADS,
        coinCost = 10
    ),
    DIY_CALCULATORS(
        title = "DIY Calculators (Paint, Tiles, Concrete, Flooring)",
        description = "Room paint volume, tile count with waste, concrete slab bags, and flooring pack calculators.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "diy_calculators"
    ),
    FINANCE_PACK(
        title = "Finance Pack Pro",
        description = "Compound interest, SIP planner, side-by-side loan comparison, savings goal, and currency alerts.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "finance_pack"
    ),
    IMAGE_TOOLKIT(
        title = "Image Toolkit Pro",
        description = "Compress, resize, convert JPG/PNG/WebP, strip EXIF & GPS location metadata, and batch process.",
        category = UnlockCategory.MEDIA_STUDIO,
        coinCost = 10,
        toolId = "image_toolkit"
    ),
    EVENT_COUNTDOWN(
        title = "Countdown & Event Timers",
        description = "Track milestones and event countdowns with live days/hours/minutes and reminder notifications.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "event_countdown"
    ),
    SPLIT_THE_BILL(
        title = "Split-the-Bill Pro",
        description = "Split itemized receipts across multiple people with proportional tax and tip.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "split_bill_pro"
    ),
    TRIP_GEAR_CHECKLISTS(
        title = "Trip & Gear Checklists",
        description = "Editable Camping, Fishing, Hunting, and Travel gear checklist templates with progress rings.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10,
        toolId = "trip_gear_checklists"
    ),
    POMODORO_STATS(
        title = "Pomodoro Statistics & Streaks",
        description = "Daily focus streaks, completed session history, total focus hours, and 7-day focus chart.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "pomodoro_stats"
    ),
    UNIT_CONVERTER_EXTRAS(
        title = "Unit Converter Extras",
        description = "Custom unit definitions, pinned favorite conversions, and RFC-4180 CSV history export.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "unit_converter_pro"
    ),
    PEDOMETER_HISTORY(
        title = "Pedometer History & Streaks",
        description = "Weekly and monthly approximate step charts, daily goal streaks, and custom targets.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10,
        toolId = "pedometer_history"
    ),
    UNLIMITED_WAYPOINTS(
        title = "Unlimited Waypoints & Bearing",
        description = "Save unlimited compass waypoints with live approximate distance, bearing, and directional arrow.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10,
        toolId = "waypoints_pro"
    ),
    PHOTO_LIGHT_METER(
        title = "Photography Light Meter",
        description = "Approximate Exposure Value (EV) calculator with suggested aperture, shutter speed, and ISO pairs.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "photo_light_meter"
    ),
    LEVEL_CALIBRATION_PROFILES(
        title = "Level Calibration Profiles",
        description = "Save, switch, and reset multiple named surface pitch & roll calibration profiles.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "level_calibration_pro"
    ),
    MEASUREMENT_HISTORY_CSV(
        title = "Measurement History & CSV Export",
        description = "Record timestamped approximate readings for Level, Angle, Compass, and Sound Meter with CSV export.",
        category = UnlockCategory.TOOLS,
        coinCost = 10,
        toolId = "measurement_history_csv"
    ),
    PHOTO_CONVERTER_PRO(
        title = "Photo Converter Pro (Batch 50 & Full Res)",
        description = "Batch convert up to 50 photos at once at full camera resolution with custom rename patterns and 4K presets.",
        category = UnlockCategory.MEDIA_STUDIO,
        coinCost = 10,
        toolId = "photo_converter_pro"
    ),
    BACKGROUND_REMOVER_PRO(
        title = "Background Remover Unlimited & HD Export",
        description = "Unlimited daily AI subject cutouts and uncompressed full-resolution HD PNG/JPG export.",
        category = UnlockCategory.MEDIA_STUDIO,
        coinCost = 10,
        toolId = "background_remover_pro"
    ),
    PHOTO_EDITOR_PRO(
        title = "Photo Editor Pro (Curves, 20 Filters & Collage)",
        description = "RGB curves, film grain, fade, all 20 ColorMatrix filters, and 2–6 photo grid collage maker.",
        category = UnlockCategory.MEDIA_STUDIO,
        coinCost = 10,
        toolId = "photo_editor_pro"
    ),
    PASSPORT_ID_PHOTO(
        title = "Passport & ID Photo Maker",
        description = "Official US, India, UK, EU, Canada & China ID sizes with face guide, white/blue backdrop, and 300 DPI 4×6 in / A4 print sheets.",
        category = UnlockCategory.MEDIA_STUDIO,
        coinCost = 10,
        toolId = "passport_id_photo"
    ),
    AUDIO_CONVERTER_PRO(
        title = "Audio Studio Pro (Unlimited, Batch, Merge & Speed/Pitch)",
        description = "Convert files over 5 minutes, batch audio conversion, multi-track audio merge, and speed/pitch shifter.",
        category = UnlockCategory.MEDIA_STUDIO,
        coinCost = 10,
        toolId = "audio_converter_pro"
    ),
    VIDEO_COMPRESS_PRO(
        title = "Video Compress & Resize (Media3)",
        description = "Hardware-accelerated H.264/AAC video compression and 1080p/720p/480p resolution scaling via Media3 Transformer.",
        category = UnlockCategory.MEDIA_STUDIO,
        coinCost = 10,
        toolId = "video_compress_pro"
    ),
    PDF_IMAGE_PRO(
        title = "Unlimited PDF to Images & Bulk Image Compress",
        description = "Render unlimited PDF pages to high-DPI JPG/PNG and batch-compress multiple photos simultaneously.",
        category = UnlockCategory.MEDIA_STUDIO,
        coinCost = 10,
        toolId = "pdf_image_pro"
    ),
    OUTDOOR_ALERTS(
        title = "Solunar, Golden-Hour & Weather Alerts",
        description = "Local WorkManager notifications for Solunar Fishing/Hunting windows (15/30/60m lead), golden hour, daily weather summary, and rain-soon alerts.",
        category = UnlockCategory.ALERTS_AND_WIDGETS,
        coinCost = 10,
        toolId = "outdoor_alerts"
    ),
    HOME_SCREEN_WIDGETS(
        title = "Home Screen Widgets (Jetpack Glance)",
        description = "Interactive home screen widgets for Flashlight toggle, Solunar score, Quick Scan, Weather, and Compass heading.",
        category = UnlockCategory.ALERTS_AND_WIDGETS,
        coinCost = 10,
        toolId = "home_widgets"
    ),
    SUN_MOON_CALENDAR(
        title = "Monthly Sun & Moon Calendar",
        description = "Full monthly lunar phase calendar with daily illumination %, Solunar activity scores, and highlighted best days.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10,
        toolId = "sun_moon_calendar"
    ),
    OUTDOOR_PLANNER_7DAY(
        title = "7-Day Outdoor Planner, Best Day & Shareable Card",
        description = "Rank the next 7 days for Fishing, Hunting, or Photography with a tide-free Best Day card and shareable day card image.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10,
        toolId = "outdoor_planner"
    ),
    AIR_QUALITY_UV_PRO(
        title = "Air Quality (AQI) & UV Index Forecast",
        description = "Cached Open-Meteo US/EU Air Quality Index, PM2.5, PM10, and UV exposure guidance.",
        category = UnlockCategory.OUTDOORS,
        coinCost = 10,
        toolId = "aqi_uv_forecast"
    ),
    CUSTOMIZATION_PRO(
        title = "Custom Accent, Tile, Carousel & Icon Styles",
        description = "Custom hex accent color picker, Glass/Solid/Outline tile styles, hero carousel layouts, and 4 icon sets.",
        category = UnlockCategory.THEMES_AND_CUSTOMIZATION,
        coinCost = 10,
        toolId = "custom_style_studio"
    ),
    FULL_VAULT_BACKUP(
        title = "Full ZIP Vault Backup & Restore",
        description = "Export and restore all documents, notes, sketches, scans, settings, and coins in one version-checked ZIP archive.",
        category = UnlockCategory.BACKUP_AND_PRIVACY,
        coinCost = 10
    ),
    APP_BIOMETRIC_LOCK(
        title = "Biometric App Lock & Recents Privacy",
        description = "App Lock (Biometric/PIN), timeout rules, hide app preview in Android Recents (FLAG_SECURE), and per-feature locks.",
        category = UnlockCategory.BACKUP_AND_PRIVACY,
        coinCost = 10
    ),
    HOME_CUSTOMIZATION(
        title = "Customize Home, Folders & Saved Layouts",
        description = "Show/hide any feature or section, drag-and-drop reorder, create custom folders, resize tiles (S/M/L), set columns (2/3/4/list), pin Android launcher shortcuts, and save up to 5 named layouts.",
        category = UnlockCategory.THEMES_AND_CUSTOMIZATION,
        coinCost = 10,
        toolId = "home_customization"
    );

    val storageKey: String
        get() = name.lowercase()

    val key: String
        get() = storageKey

    val shortBenefit: String
        get() = description

    companion object {
        fun fromToolId(toolId: String): PremiumFeature? {
            val normalized = toolId.trim().lowercase()
            return when (normalized) {
                "angle", "angle_pro" -> ANGLE
                "metal_finder", "metal_detector" -> METAL_DETECTOR
                "sound_db", "sound_meter" -> SOUND_METER
                "walk_run_tracker" -> WALK_RUN_TRACKER
                "altimeter" -> ALTIMETER
                "vibration_gforce" -> VIBRATION_GFORCE
                "fishing_log" -> FISHING_LOG
                "tip_calc", "scientific_diy_calc", "scientific_calc" -> SCIENTIFIC_DIY_CALCULATORS
                "diy_calculators" -> DIY_CALCULATORS
                "finance_pack" -> FINANCE_PACK
                "image_toolkit" -> IMAGE_TOOLKIT
                "event_countdown" -> EVENT_COUNTDOWN
                "split_bill_pro" -> SPLIT_THE_BILL
                "trip_gear_checklists" -> TRIP_GEAR_CHECKLISTS
                "pomodoro_stats" -> POMODORO_STATS
                "unit_converter_pro" -> UNIT_CONVERTER_EXTRAS
                "pedometer_history" -> PEDOMETER_HISTORY
                "waypoints_pro", "unlimited_waypoints" -> UNLIMITED_WAYPOINTS
                "photo_light_meter" -> PHOTO_LIGHT_METER
                "level_calibration_pro" -> LEVEL_CALIBRATION_PROFILES
                "measurement_history_csv" -> MEASUREMENT_HISTORY_CSV
                "interval_timer" -> INTERVAL_TIMER
                "noise_sleep_mixer" -> NOISE_SLEEP_MIXER
                "tuner" -> TUNER
                "qr_generator_styles", "qr_studio_pro" -> QR_GENERATOR_STYLES
                "photo_converter_pro" -> PHOTO_CONVERTER_PRO
                "background_remover_pro" -> BACKGROUND_REMOVER_PRO
                "photo_editor_pro", "photo_collage_pro" -> PHOTO_EDITOR_PRO
                "passport_id_photo" -> PASSPORT_ID_PHOTO
                "audio_converter_pro", "audio_studio_pro" -> AUDIO_CONVERTER_PRO
                "video_compress_pro" -> VIDEO_COMPRESS_PRO
                "pdf_image_pro" -> PDF_IMAGE_PRO
                "outdoor_alerts", "solunar_alerts" -> OUTDOOR_ALERTS
                "home_widgets", "glance_widgets" -> HOME_SCREEN_WIDGETS
                "sun_moon_calendar", "lunar_calendar" -> SUN_MOON_CALENDAR
                "outdoor_planner", "best_day_planner" -> OUTDOOR_PLANNER_7DAY
                "aqi_uv_forecast", "air_quality_uv" -> AIR_QUALITY_UV_PRO
                "custom_style_studio", "accent_color_picker" -> CUSTOMIZATION_PRO
                "home_customization", "customize_home" -> HOME_CUSTOMIZATION
                else -> entries.firstOrNull { it.toolId.equals(normalized, ignoreCase = true) }
            }
        }

        fun groupedByCategory(): Map<UnlockCategory, List<PremiumFeature>> {
            return UnlockCategory.entries.associateWith { cat ->
                entries.filter { it.category == cat }
            }
        }
    }
}

enum class DocumentPackFeature(
    val title: String,
    val description: String
) {
    PDF_COMPRESS(
        title = "PDF Compress (Low / Medium / High)",
        description = "Re-encode and optimize PDF pages with Low, Medium, or High compression and exact before/after size comparison."
    ),
    PDF_PASSWORD(
        title = "Password Protect & Unlock PDF",
        description = "Encrypt PDFs with standard password protection or unlock protected PDFs using Apache-2.0 PdfBox-Android."
    ),
    PDF_WATERMARK(
        title = "PDF Watermark (Text or Image)",
        description = "Add custom text or image watermarks with adjustable opacity and position across all pages."
    ),
    PDF_PAGE_NUMBERS(
        title = "PDF Page Numbers",
        description = "Stamp automatic 'Page X of N' numbering at Top or Bottom left, center, or right."
    ),
    PDF_HEADER_FOOTER(
        title = "PDF Header & Footer",
        description = "Apply custom document headers and footers across every page of your PDF."
    ),
    PDF_STAMPS(
        title = "Official PDF Stamps",
        description = "Place crisp vector stamps: APPROVED, PAID, DRAFT, or CONFIDENTIAL."
    ),
    PDF_CROP(
        title = "Crop PDF Pages",
        description = "Trim margins or crop custom regions on individual or all PDF pages."
    ),
    PDF_REDACT(
        title = "Permanent Redact & Flatten",
        description = "Draw blackout redaction boxes and flatten pages into bitmaps so hidden content is permanently destroyed."
    ),
    PDF_UNLIMITED_MERGE(
        title = "Unlimited PDF Merge",
        description = "Free merges up to 3 files; Premium Document Pack merges unlimited PDFs."
    ),
    PDF_SAVED_SIGNATURES(
        title = "Up to 5 Saved Signatures & Initials",
        description = "Store and switch between up to 5 reusable handwritten signatures and initials."
    ),
    PDF_FORM_SNIPPETS(
        title = "Form-Fill Snippets (Name, Address, Date)",
        description = "One-tap insertion of saved Name, Address, and Date snippets onto any PDF form."
    ),
    SMART_SCAN_FILTERS(
        title = "Pro Scan Filters & Sliders",
        description = "Magic Color, B&W Adaptive Threshold, Grayscale, Whiteboard, Shadow Removal, Brightness/Contrast/Sharpen sliders & Batch Apply."
    ),
    SMART_ID_CARD_MODE(
        title = "ID Card Mode (Front + Back)",
        description = "Capture ID front and back and place both on one page at true ID-1 size (85.6 × 54 mm)."
    ),
    SMART_RECEIPT_EXPENSE(
        title = "Receipt & Expense Tracker",
        description = "OCR parses merchant, date, and total with category chips, Canvas monthly bar chart, and CSV export."
    ),
    SMART_BUSINESS_CARD(
        title = "Business Card Scanner to Contacts",
        description = "OCR extracts name, phone, email, and company, then opens 'Save to contacts'."
    ),
    SMART_OCR_ACTIONS(
        title = "Full Multi-Page OCR, Read Aloud & Translate",
        description = "Full-document text recognition, Copy, Share as TXT, Read Aloud, and offline translation."
    ),
    SMART_LIBRARY_ORG(
        title = "Folders, Tags, Bulk Actions & ZIP Export",
        description = "Organize with folders and tags, smart OCR search, bulk select (share/delete/move/merge), and batch ZIP export."
    ),
    SMART_EXPIRY_REMINDERS(
        title = "ID & Bill Expiry Reminders",
        description = "Schedule local reminders 30, 7, and 1 day before an ID or bill expires."
    ),
    PDF_PASSWORD_PROTECT(
        title = "Password Protect & Unlock PDF",
        description = "Encrypt PDFs with standard password protection or unlock protected PDFs using Apache-2.0 PdfBox-Android."
    ),
    SMART_FOLDERS_TAGS(
        title = "Folders, Tags, Bulk Actions & ZIP Export",
        description = "Organize with folders and tags, smart OCR search, bulk select (share/delete/move/merge), and batch ZIP export."
    )
}

sealed class FeatureAccessResult {
    data object Allowed : FeatureAccessResult()
    data class Locked(
        val feature: com.example.domain.Feature,
        val premiumFeature: PremiumFeature
    ) : FeatureAccessResult()
}

typealias UnlockPromptState = UnlockSheetRequest

data class UnlockSheetRequest(
    val feature: PremiumFeature,
    val catalogFeatureId: String? = null,
    val featureKey: String? = null,
    val themeName: String? = null,
    val soundPack: SoundPack? = null,
    val quotaType: QuotaType? = null,
    val customTitle: String? = null,
    val customDescription: String? = null,
    val onUnlocked: () -> Unit = {}
) {
    val displayTitle: String
        get() = when {
            !customTitle.isNullOrBlank() -> customTitle
            quotaType != null -> "${quotaType.displayTitle} (${quotaType.dailyAllowance}/${quotaType.dailyAllowance} Free Used)"
            soundPack != null -> soundPack.title
            feature == PremiumFeature.PREMIUM_THEMES && !themeName.isNullOrBlank() -> "$themeName Theme"
            else -> feature.title
        }

    val displayDescription: String
        get() = when {
            !customDescription.isNullOrBlank() -> customDescription
            quotaType != null ->
                "You've used all ${quotaType.dailyAllowance} free uses for today. Your free allowance resets automatically at local midnight — or watch a short ad (+1 use), use 10 coins, or Go Premium for unlimited access."
            soundPack != null -> soundPack.subtitle
            else -> feature.description
        }

    val isOneShotConsumable: Boolean
        get() = quotaType != null ||
            feature == PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE ||
            feature == PremiumFeature.CSV_EXPORT

    val coinCost: Int
        get() = 10

    val effectiveUnlockKey: String
        get() = when {
            soundPack != null -> AccessManager.normalizeKey(soundPack.productId)
            !themeName.isNullOrBlank() -> AccessManager.themeKey(themeName)
            !featureKey.isNullOrBlank() -> AccessManager.normalizeKey(featureKey)
            !catalogFeatureId.isNullOrBlank() -> AccessManager.normalizeKey(catalogFeatureId)
            feature == PremiumFeature.HOME_CUSTOMIZATION || feature == PremiumFeature.CUSTOMIZATION_PRO ->
                AccessManager.KEY_CUSTOMIZE
            else -> AccessManager.normalizeKey(feature.storageKey)
        }
}

/**
 * Central FeatureGate backed by [AccessManager] (single source of truth):
 * - hasAccess(featureKey) = isPremium OR unlockExpiry[featureKey] > now.
 * - Keys are strictly per feature (feature.id), per theme ("theme:<id>"), and "customize".
 * - An unlock grants ONLY its own key; there is no global unlock flag, no shared timer, and no 10-second preview.
 */
class FeatureGate(
    val purchaseManager: PurchaseManager,
    private val preferencesRepository: PreferencesRepository,
    private val scanRepository: ScanRepository? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val quotaManager: QuotaManager = QuotaManager(preferencesRepository)

    private val _latestPrefs = MutableStateFlow(UserPreferences())
    val latestPrefs: StateFlow<UserPreferences> = _latestPrefs.asStateFlow()

    private val _activeUnlockRequest = MutableStateFlow<UnlockSheetRequest?>(null)
    val activeUnlockRequest: StateFlow<UnlockSheetRequest?> = _activeUnlockRequest.asStateFlow()

    private val _unlockStatusNotice = MutableStateFlow<String?>(null)
    val unlockStatusNotice: StateFlow<String?> = _unlockStatusNotice.asStateFlow()

    init {
        scope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                _latestPrefs.value = prefs
            }
        }
    }

    val isPro: StateFlow<Boolean> = purchaseManager.isPro
    val ownedSoundPacks: StateFlow<Set<String>> = purchaseManager.ownedSoundPacks
    val soundPackPrices: StateFlow<Map<String, String>> = purchaseManager.soundPackPrices

    fun launchSoundPackPurchase(activity: android.app.Activity, pack: SoundPack) {
        purchaseManager.launchSoundPackPurchase(activity, pack.productId)
    }

    /**
     * Authoritative per-key access check via [AccessManager].
     */
    fun hasAccess(
        featureKey: String,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = AccessManager.currentElapsedRealtimeMs()
    ): Boolean {
        val clean = AccessManager.normalizeKey(featureKey)
        if (clean.isEmpty()) return false
        val isProActive = purchaseManager.isPro.value || prefs.cachedIsPro || prefs.simulateProEnabled
        if (isProActive) return true

        if (clean.startsWith(AccessManager.THEME_PREFIX)) {
            val themeName = clean.removePrefix(AccessManager.THEME_PREFIX)
            if (themeName.equals("lime night", ignoreCase = true)) return true
            return AccessManager.hasAccess(
                featureKey = clean,
                isPremium = false,
                prefs = prefs,
                nowWallMs = nowMs,
                nowElapsedMs = nowElapsedMs
            )
        }

        if (clean == AccessManager.KEY_CUSTOMIZE ||
            clean == PremiumFeature.HOME_CUSTOMIZATION.storageKey
        ) {
            return AccessManager.hasAccess(
                featureKey = AccessManager.KEY_CUSTOMIZE,
                isPremium = false,
                prefs = prefs,
                nowWallMs = nowMs,
                nowElapsedMs = nowElapsedMs
            )
        }

        val catalogFeature = com.example.domain.FeatureRegistry.getFeatureById(clean)
        if (catalogFeature != null && !catalogFeature.isPremium) {
            return true
        }

        if (purchaseManager.ownedSoundPacks.value.contains(clean) ||
            prefs.ownedSoundPacksSet.contains(clean)
        ) {
            return true
        }

        if (prefs.isToolSpotlightUnlocked(clean, nowMs)) {
            return true
        }

        return AccessManager.hasAccess(
            featureKey = clean,
            isPremium = false,
            prefs = prefs,
            nowWallMs = nowMs,
            nowElapsedMs = nowElapsedMs
        )
    }

    /**
     * Checks whether a [SoundPack] is unlocked via Dyne Premium, Play Billing pack purchase,
     * or an active 1-hour temporary unlock for that specific pack.
     */
    fun isPackUnlocked(
        pack: SoundPack,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val isProActive = purchaseManager.isPro.value || prefs.cachedIsPro || prefs.simulateProEnabled
        if (isProActive) return true
        val lowerId = pack.productId.lowercase()
        if (purchaseManager.ownedSoundPacks.value.contains(lowerId)) return true
        if (prefs.ownedSoundPacksSet.contains(lowerId)) return true
        return AccessManager.hasAccess(
            featureKey = lowerId,
            isPremium = false,
            prefs = prefs,
            nowWallMs = nowMs
        )
    }

    /**
     * Checks whether [feature] (or a specific [themeName] when [feature] == PREMIUM_THEMES)
     * is currently unlocked via Dyne Pro or an active 1-hour temporary unlock for its specific key.
     */
    fun isUnlocked(
        feature: PremiumFeature,
        themeName: String? = null,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val isProActive = purchaseManager.isPro.value || prefs.cachedIsPro || prefs.simulateProEnabled
        if (isProActive) return true
        if (feature == PremiumFeature.NO_ADS) return false

        if (feature == PremiumFeature.PREMIUM_THEMES && !themeName.isNullOrBlank()) {
            if (themeName.equals("Lime Night", ignoreCase = true)) return true
            return AccessManager.hasAccess(
                featureKey = AccessManager.themeKey(themeName),
                isPremium = false,
                prefs = prefs,
                nowWallMs = nowMs
            )
        }

        if (feature == PremiumFeature.HOME_CUSTOMIZATION || feature == PremiumFeature.CUSTOMIZATION_PRO) {
            return AccessManager.hasAccess(
                featureKey = AccessManager.KEY_CUSTOMIZE,
                isPremium = false,
                prefs = prefs,
                nowWallMs = nowMs
            )
        }

        return AccessManager.hasAccess(
            featureKey = feature.storageKey,
            isPremium = false,
            prefs = prefs,
            nowWallMs = nowMs
        )
    }

    fun isDocumentPackUnlocked(
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        return isUnlocked(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE, prefs = prefs, nowMs = nowMs)
    }

    fun isDocumentPackUnlocked(
        docFeature: DocumentPackFeature,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val isProActive = purchaseManager.isPro.value || prefs.cachedIsPro || prefs.simulateProEnabled
        if (isProActive) return true
        return AccessManager.hasAccess(
            featureKey = docFeature.name.lowercase(),
            isPremium = false,
            prefs = prefs,
            nowWallMs = nowMs
        ) || isDocumentPackUnlocked(prefs = prefs, nowMs = nowMs)
    }

    fun isToolUnlocked(
        toolId: String,
        isProTool: Boolean = true,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!isProTool) return true
        return hasAccess(toolId, prefs = prefs, nowMs = nowMs)
    }

    fun isCatalogFeatureUnlocked(
        catalogFeature: com.example.domain.Feature,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = AccessManager.safeElapsedRealtime()
    ): Boolean {
        if (!catalogFeature.isPremium) return true
        val isProActive = purchaseManager.isPro.value || prefs.cachedIsPro || prefs.simulateProEnabled
        if (isProActive) return true
        if (prefs.isToolSpotlightUnlocked(catalogFeature.id, nowMs)) {
            return true
        }
        return AccessManager.hasAccess(
            featureKey = catalogFeature.id,
            isPremium = false,
            prefs = prefs,
            nowWallMs = nowMs,
            nowElapsedMs = nowElapsedMs
        )
    }

    fun isHomeCustomizationUnlocked(
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val isProActive = purchaseManager.isPro.value || prefs.cachedIsPro || prefs.simulateProEnabled
        if (isProActive) return true
        return AccessManager.hasAccess(
            featureKey = AccessManager.KEY_CUSTOMIZE,
            isPremium = false,
            prefs = prefs,
            nowWallMs = nowMs
        )
    }

    /**
     * Route-level access resolution for any feature ID or tool ID (`"feature/{id}"`).
     * Strictly checks per-feature access (`catalogFeature.id`).
     * Unlocking `flashlight_strobe` does NOT unlock `flashlight_morse` or any other feature.
     */
    fun resolve(
        featureIdOrToolId: String,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): FeatureAccessResult {
        val cleanId = featureIdOrToolId.trim().removePrefix("feature/")
        val feature = com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(cleanId)
            ?: return FeatureAccessResult.Allowed
        if (!feature.isPremium) {
            return FeatureAccessResult.Allowed
        }
        if (isCatalogFeatureUnlocked(feature, prefs = prefs, nowMs = nowMs)) {
            return FeatureAccessResult.Allowed
        }
        val pf = feature.premiumFeature
            ?: PremiumFeature.fromToolId(feature.toolId)
            ?: PremiumFeature.CUSTOMIZATION_PRO
        return FeatureAccessResult.Locked(feature = feature, premiumFeature = pf)
    }

    /**
     * Guarded navigation helper that delegates to [FeatureGuard.navigate].
     * Locked = opens the Unlock sheet for THAT exact featureKey and does NOT navigate.
     */
    fun guardNavigate(
        featureKey: String,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = AccessManager.safeElapsedRealtime(),
        onNavigate: ((String) -> Unit)? = null,
        onAllowed: (String) -> Unit = { key -> onNavigate?.invoke(key) }
    ): Boolean {
        val navigateCallback: (String) -> Unit = onNavigate ?: onAllowed
        val clean = AccessManager.normalizeKey(featureKey.removePrefix("feature/"))
        val resolvedCatalog = com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(clean)
        val canonicalKey = resolvedCatalog?.id ?: clean
        return FeatureGuard.navigate(
            featureKey = canonicalKey,
            hasAccess = { key ->
                if (resolvedCatalog != null && key == resolvedCatalog.id) {
                    isCatalogFeatureUnlocked(resolvedCatalog, prefs = prefs, nowMs = nowMs, nowElapsedMs = nowElapsedMs)
                } else {
                    hasAccess(key, prefs = prefs, nowMs = nowMs, nowElapsedMs = nowElapsedMs)
                }
            },
            onOpenUnlockSheet = { key ->
                _unlockStatusNotice.value = null
                when {
                    key.startsWith(AccessManager.THEME_PREFIX) -> {
                        val tName = key.removePrefix(AccessManager.THEME_PREFIX)
                            .replaceFirstChar { it.uppercase() }
                        _activeUnlockRequest.value = UnlockSheetRequest(
                            feature = PremiumFeature.PREMIUM_THEMES,
                            featureKey = key,
                            themeName = tName,
                            customTitle = "$tName Theme",
                            customDescription = "Try the $tName glass theme for 1 hour, or upgrade to Dyne Premium.",
                            onUnlocked = { navigateCallback(canonicalKey) }
                        )
                    }
                    key == AccessManager.KEY_CUSTOMIZE -> {
                        _activeUnlockRequest.value = UnlockSheetRequest(
                            feature = PremiumFeature.HOME_CUSTOMIZATION,
                            featureKey = AccessManager.KEY_CUSTOMIZE,
                            customTitle = PremiumFeature.HOME_CUSTOMIZATION.title,
                            customDescription = PremiumFeature.HOME_CUSTOMIZATION.description,
                            onUnlocked = { navigateCallback(canonicalKey) }
                        )
                    }
                    resolvedCatalog != null -> {
                        val pf = resolvedCatalog.premiumFeature
                            ?: PremiumFeature.fromToolId(resolvedCatalog.toolId)
                            ?: PremiumFeature.CUSTOMIZATION_PRO
                        _activeUnlockRequest.value = UnlockSheetRequest(
                            feature = pf,
                            catalogFeatureId = resolvedCatalog.id,
                            featureKey = resolvedCatalog.id,
                            customTitle = resolvedCatalog.title,
                            customDescription = resolvedCatalog.shortBenefit,
                            onUnlocked = { navigateCallback(resolvedCatalog.id) }
                        )
                    }
                    else -> {
                        val pf = PremiumFeature.fromToolId(key) ?: PremiumFeature.CUSTOMIZATION_PRO
                        _activeUnlockRequest.value = UnlockSheetRequest(
                            feature = pf,
                            featureKey = key,
                            customTitle = pf.title,
                            customDescription = pf.description,
                            onUnlocked = { navigateCallback(canonicalKey) }
                        )
                    }
                }
            },
            onNavigate = navigateCallback
        )
    }

    fun requireCatalogFeature(
        catalogFeature: com.example.domain.Feature,
        onUnlocked: () -> Unit = {}
    ) {
        guardNavigate(
            featureKey = catalogFeature.id,
            onNavigate = { onUnlocked() }
        )
    }

    fun isThemeUnlocked(
        themeName: String,
        isProTheme: Boolean = true,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!isProTheme) return true
        val isProActive = purchaseManager.isPro.value || prefs.cachedIsPro || prefs.simulateProEnabled
        if (isProActive) return true
        return isUnlocked(
            feature = PremiumFeature.PREMIUM_THEMES,
            themeName = themeName,
            prefs = prefs,
            nowMs = nowMs
        )
    }

    fun getActive1hUnlockDescriptions(
        nowMs: Long = System.currentTimeMillis(),
        prefs: UserPreferences = _latestPrefs.value
    ): List<String> {
        val activeMap = prefs.getAllActiveTemporaryUnlocks(nowMs)
        return activeMap.entries.map { (key, expMs) ->
            val remStr = formatRemainingShort((expMs - nowMs).coerceAtLeast(0L))
            "${resolveFriendlyFeatureName(key)} ($remStr left)"
        }
    }

    /**
     * Routes an entry point through FeatureGate:
     * - If unlocked, runs [onUnlocked] immediately.
     * - If locked, opens the 1-hour Unlock sheet and does NOT execute [onUnlocked] behind the sheet.
     */
    fun require(
        feature: PremiumFeature,
        featureKey: String? = null,
        themeName: String? = null,
        customTitle: String? = null,
        customDescription: String? = null,
        onUnlocked: () -> Unit = {}
    ) {
        val targetKey = when {
            !themeName.isNullOrBlank() -> AccessManager.themeKey(themeName)
            !featureKey.isNullOrBlank() -> AccessManager.normalizeKey(featureKey)
            feature == PremiumFeature.HOME_CUSTOMIZATION || feature == PremiumFeature.CUSTOMIZATION_PRO ->
                AccessManager.KEY_CUSTOMIZE
            else -> feature.storageKey
        }
        if (hasAccess(targetKey)) {
            onUnlocked()
        } else {
            _unlockStatusNotice.value = null
            _activeUnlockRequest.value = UnlockSheetRequest(
                feature = feature,
                featureKey = targetKey,
                themeName = themeName,
                customTitle = customTitle,
                customDescription = customDescription,
                onUnlocked = onUnlocked
            )
        }
    }

    /**
     * Routes a Premium Document Pack feature (PDF Tools or Smart Document Tools) through FeatureGate.
     */
    fun requireDocumentPack(
        docFeature: DocumentPackFeature,
        onUnlocked: () -> Unit = {}
    ) {
        val docKey = docFeature.name.lowercase()
        if (isDocumentPackUnlocked(docFeature)) {
            onUnlocked()
        } else {
            _unlockStatusNotice.value = null
            _activeUnlockRequest.value = UnlockSheetRequest(
                feature = PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE,
                featureKey = docKey,
                customTitle = docFeature.title,
                customDescription = docFeature.description,
                onUnlocked = onUnlocked
            )
        }
    }

    /**
     * Routes a Sound Pack or Frequency template/control through FeatureGate.
     */
    fun requirePack(
        pack: SoundPack,
        customTitle: String? = null,
        customDescription: String? = null,
        onUnlocked: () -> Unit = {}
    ) {
        if (isPackUnlocked(pack)) {
            onUnlocked()
        } else {
            _unlockStatusNotice.value = null
            _activeUnlockRequest.value = UnlockSheetRequest(
                feature = PremiumFeature.FREQUENCY_PRO,
                soundPack = pack,
                featureKey = pack.productId.lowercase(),
                customTitle = customTitle,
                customDescription = customDescription,
                onUnlocked = onUnlocked
            )
        }
    }

    /**
     * Checks daily free allowance for [quotaType] (resets at local midnight).
     * - If Premium or the associated feature is unlocked, runs [onAllowed] immediately.
     * - If free uses or bonus credits remain today, consumes 1 use and runs [onAllowed].
     * - At the limit, opens the Unlock sheet offering: Watch an ad (+1 use), Use 10 coins, or Go Premium.
     */
    fun requireQuotaOrUnlock(
        quotaType: QuotaType,
        onAllowed: () -> Unit
    ) {
        if (isUnlocked(quotaType.associatedFeature)) {
            onAllowed()
            return
        }
        val status = quotaManager.getStatus(quotaType, _latestPrefs.value)
        if (!status.isAtLimit) {
            scope.launch {
                val consumed = quotaManager.consumeUse(quotaType)
                if (consumed) {
                    onAllowed()
                } else {
                    openQuotaUnlockSheet(quotaType, onUnlocked = onAllowed)
                }
            }
        } else {
            openQuotaUnlockSheet(quotaType, onUnlocked = onAllowed)
        }
    }

    fun openQuotaUnlockSheet(
        quotaType: QuotaType,
        onUnlocked: () -> Unit = {}
    ) {
        _unlockStatusNotice.value = null
        _activeUnlockRequest.value = UnlockSheetRequest(
            feature = quotaType.associatedFeature,
            quotaType = quotaType,
            onUnlocked = {
                scope.launch {
                    quotaManager.consumeUse(quotaType)
                    onUnlocked()
                }
            }
        )
    }

    fun dismissUnlockSheet() {
        _activeUnlockRequest.value = null
        _unlockStatusNotice.value = null
    }

    fun setUnlockSheetNotice(message: String?) {
        _unlockStatusNotice.value = message
    }

    fun getFeatureExpiryMs(
        feature: PremiumFeature,
        themeName: String? = null,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Long? {
        if (feature == PremiumFeature.PREMIUM_THEMES && !themeName.isNullOrBlank()) {
            return prefs.getThemeRewardExpiryMs(themeName, nowMs)
        }
        if (feature == PremiumFeature.HOME_CUSTOMIZATION || feature == PremiumFeature.CUSTOMIZATION_PRO) {
            return prefs.getFeatureUnlockExpiryMs(AccessManager.KEY_CUSTOMIZE, nowMs)
        }
        return prefs.getFeatureUnlockExpiryMs(feature.storageKey, nowMs)
    }

    fun getRemainingMsForKey(
        featureKey: String,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        if (purchaseManager.isPro.value || prefs.cachedIsPro || prefs.simulateProEnabled) return 0L
        return AccessManager.getRemainingUnlockMs(
            featureKey = featureKey,
            prefs = prefs,
            nowWallMs = nowMs
        )
    }

    fun getCountdownProgressForKey(
        featureKey: String,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Float {
        val rem = getRemainingMsForKey(featureKey, prefs, nowMs)
        if (rem <= 0L) return 0f
        return (rem.toFloat() / UserPreferences.ONE_HOUR_MS.toFloat()).coerceIn(0.02f, 1f)
    }

    fun getRemainingMs(
        feature: PremiumFeature,
        themeName: String? = null,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val exp = getFeatureExpiryMs(feature, themeName, prefs, nowMs) ?: return 0L
        return (exp - nowMs).coerceAtLeast(0L)
    }

    fun getCountdownProgress(
        feature: PremiumFeature,
        themeName: String? = null,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Float {
        val rem = getRemainingMs(feature, themeName, prefs, nowMs)
        if (rem <= 0L) return 0f
        return (rem.toFloat() / UserPreferences.ONE_HOUR_MS.toFloat()).coerceIn(0.02f, 1f)
    }

    fun formatRemainingShort(remainingMs: Long): String {
        if (remainingMs <= 0L) return "0m"
        val totalMinutes = (remainingMs / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        return when {
            hours > 0L && mins > 0L -> "${hours}h ${mins}m"
            hours > 0L -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun formatRemainingRingBadge(remainingMs: Long): String {
        if (remainingMs <= 0L) return "0m"
        val totalMinutes = (remainingMs / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        return if (hours >= 1L) "${hours}h" else "${totalMinutes}m"
    }

    /**
     * Returns any active 1h unlock that has 5 minutes or less remaining so a notice banner can be shown.
     */
    fun getExpiringSoonNotice(
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): String? {
        if (purchaseManager.isPro.value) return null
        val activeMap = prefs.getAllActiveTemporaryUnlocks(nowMs)
        for ((key, exp) in activeMap) {
            val rem = exp - nowMs
            if (rem in 1L..UserPreferences.FIVE_MINUTES_MS) {
                val mins = (rem / 60_000L).coerceAtLeast(1L)
                val friendlyName = resolveFriendlyFeatureName(key)
                return "$friendlyName temporary unlock expires in ${mins}m"
            }
        }
        return null
    }

    fun resolveFriendlyFeatureName(rawKey: String): String {
        val clean = rawKey.trim().lowercase()
        if (clean.startsWith("theme:")) {
            val name = clean.removePrefix("theme:").replaceFirstChar { it.uppercase() }
            return "$name Theme"
        }
        if (clean == AccessManager.KEY_CUSTOMIZE) {
            return "Home & Style Customization"
        }
        com.example.domain.FeatureRegistry.getFeatureById(clean)?.let { return it.title }
        SoundPack.fromProductId(clean)?.let { return it.title }
        PremiumFeature.entries.firstOrNull {
            it.storageKey == clean || it.toolId?.lowercase() == clean
        }?.let { return it.title }
        return rawKey.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    /**
     * Returns the remaining 1-hour unlock time in ms ONLY for the exact [catalogFeature] (or [fallbackFeature] key).
     * Never bleeds across sibling features sharing a toolId or PremiumFeature enum.
     */
    fun getTemporaryUnlockRemainingMsForFeature(
        catalogFeature: com.example.domain.Feature?,
        fallbackFeature: PremiumFeature? = null,
        toolId: String? = null,
        prefs: UserPreferences = _latestPrefs.value,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        if (purchaseManager.isPro.value || prefs.cachedIsPro || prefs.simulateProEnabled) return 0L
        if (catalogFeature != null) {
            if (!catalogFeature.isPremium) return 0L
            val directRem = AccessManager.getRemainingUnlockMs(
                featureKey = catalogFeature.id,
                prefs = prefs,
                nowWallMs = nowMs
            )
            val spotlightRem = if (prefs.isToolSpotlightUnlocked(catalogFeature.id, nowMs)) {
                (prefs.spotlightExpiryEpochMs - nowMs).coerceAtLeast(0L)
            } else {
                0L
            }
            return maxOf(directRem, spotlightRem)
        }
        if (fallbackFeature != null) {
            return AccessManager.getRemainingUnlockMs(
                featureKey = fallbackFeature.storageKey,
                prefs = prefs,
                nowWallMs = nowMs
            )
        }
        if (!toolId.isNullOrBlank()) {
            return AccessManager.getRemainingUnlockMs(
                featureKey = toolId,
                prefs = prefs,
                nowWallMs = nowMs
            )
        }
        return 0L
    }

    /**
     * Called ONLY from RewardedAd's onUserEarnedReward callback to grant a 1-hour temporary unlock
     * for ONLY [request.effectiveUnlockKey].
     */
    fun grant1HourUnlockFromRewardedAd(
        request: UnlockSheetRequest,
        onComplete: () -> Unit = { request.onUnlocked() }
    ) {
        scope.launch {
            preferencesRepository.recordAdOrCoinPremiumUse()
            if (request.quotaType != null) {
                preferencesRepository.grantQuotaBonusUse(request.quotaType.key, delta = 1)
            } else {
                preferencesRepository.unlockFeatureFor1Hour(
                    featureKey = request.effectiveUnlockKey,
                    themeName = request.themeName
                )
            }
            if (request.feature == PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) {
                preferencesRepository.grantOneExtraDocumentSave()
            } else if (request.feature == PremiumFeature.CSV_EXPORT) {
                preferencesRepository.grantOneExtraCsvExport()
            }
            _activeUnlockRequest.value = null
            _unlockStatusNotice.value = null
            onComplete()
        }
    }

    /**
     * Spends 10 coins from DataStore, records the transaction in the Room ledger,
     * and grants a 1h unlock for ONLY [request.effectiveUnlockKey] (or +1 quota use).
     */
    fun unlockWithCoins(
        request: UnlockSheetRequest,
        onComplete: () -> Unit,
        onInsufficientCoins: (needed: Int, currentBalance: Int) -> Unit
    ) {
        scope.launch {
            val cost = request.coinCost
            val (spent, newBalance) = preferencesRepository.spendCoins(cost)
            if (!spent) {
                onInsufficientCoins(cost - newBalance, newBalance)
                return@launch
            }

            preferencesRepository.recordAdOrCoinPremiumUse()
            scanRepository?.recordCoinTransaction(
                delta = -cost,
                balanceAfter = newBalance,
                reason = if (request.quotaType != null) {
                    "+1 Use for ${request.quotaType.displayTitle}"
                } else {
                    "Unlocked ${request.displayTitle} (1h)"
                }
            )
            if (request.quotaType != null) {
                preferencesRepository.grantQuotaBonusUse(request.quotaType.key, delta = 1)
            } else {
                preferencesRepository.unlockFeatureFor1Hour(
                    featureKey = request.effectiveUnlockKey,
                    themeName = request.themeName
                )
            }
            if (request.feature == PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) {
                preferencesRepository.grantOneExtraDocumentSave()
            } else if (request.feature == PremiumFeature.CSV_EXPORT) {
                preferencesRepository.grantOneExtraCsvExport()
            }
            _activeUnlockRequest.value = null
            _unlockStatusNotice.value = null
            onComplete()
        }
    }

    fun unlockWithCoins(
        request: UnlockSheetRequest,
        onResult: (Boolean, String) -> Unit
    ) {
        unlockWithCoins(
            request = request,
            onComplete = {
                request.onUnlocked()
                val msg = if (request.quotaType != null) {
                    "+1 use unlocked for ${request.quotaType.displayTitle}!"
                } else {
                    "Unlocked ${request.displayTitle} for 1 hour!"
                }
                onResult(true, msg)
            },
            onInsufficientCoins = { _, currentBal ->
                onResult(
                    false,
                    "Need ${request.coinCost} coins (you have $currentBal coins). Watch an ad or check in daily to earn more."
                )
            }
        )
    }
}

val LocalFeatureGate = staticCompositionLocalOf<FeatureGate?> { null }
