package com.example.domain

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.MainActivity
import com.example.R
import com.example.billing.PremiumFeature
import com.example.data.DeviceCapabilities
import com.example.data.HardwareCapability
import org.json.JSONArray
import org.json.JSONObject

/**
 * Maximum number of Favorites allowed on the Free tier (0 = Favorites require Premium or a 1-hour unlock).
 */
const val FREE_FAVORITES_LIMIT = 0

/**
 * Maximum number of named Home layouts a Premium user can save.
 */
const val MAX_SAVED_HOME_LAYOUTS = 5

/**
 * Represents a single visible feature, tool mode, or sub-feature in the app.
 * Every mode and sub-feature is registered once here as the single source of truth.
 */
@Immutable
data class Feature(
    val id: String,
    val toolId: String,
    val modeId: String? = null,
    val title: String,
    val shortBenefit: String,
    val icon: ImageVector,
    val category: ToolCategory,
    val isPremium: Boolean,
    val requiredCapabilities: Set<HardwareCapability> = emptySet(),
    val aliases: List<String> = emptyList(),
    val premiumFeature: PremiumFeature? = null
) {
    val route: String
        get() = "feature/$id"

    val destinationKey: String
        get() = id
}

/**
 * Tile sizes available when customizing Home (Premium).
 */
enum class FeatureTileSize(val code: String, val label: String) {
    SMALL("S", "Small (S)"),
    MEDIUM("M", "Medium (M)"),
    LARGE("L", "Large (L)");

    companion object {
        fun fromCode(code: String?): FeatureTileSize =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MEDIUM
    }
}

/**
 * Per-section column or list display modes when customizing Home (Premium).
 */
enum class SectionLayoutStyle(val code: String, val label: String, val columns: Int) {
    ROW("row", "Horizontal Row", 0),
    GRID_2("2", "2 Columns", 2),
    GRID_3("3", "3 Columns", 3),
    GRID_4("4", "4 Columns", 4),
    LIST("list", "Vertical List", 1);

    companion object {
        fun fromCode(code: String?): SectionLayoutStyle =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ROW
    }
}

/**
 * Custom or built-in Home section / folder configuration (Premium).
 */
data class CustomHomeSection(
    val id: String,
    val name: String,
    val colorHex: Long = 0xFF38BDF8,
    val iconName: String = "Folder",
    val isBuiltIn: Boolean = false,
    val builtInCategory: ToolCategory? = null,
    val isHidden: Boolean = false,
    val layoutStyle: SectionLayoutStyle = SectionLayoutStyle.ROW,
    val featureIds: List<String> = emptyList()
)

/**
 * Named saved Home layout preset (up to 5: e.g., Outdoor, Office, Travel).
 */
data class SavedHomeLayout(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val sectionsJson: String,
    val hiddenFeatureIdsCsv: String,
    val perTileSizeCsv: String,
    val pinnedTopFeatureIdsCsv: String,
    val startTabIndex: Int,
    val homeFirstFeatureId: String
)

/**
 * Single source of truth for every tool mode and sub-feature across Dyne.
 */
object FeatureRegistry {

    const val EXTRA_SHORTCUT_FEATURE_ID = "extra_shortcut_feature_id"

    val allFeatures: List<Feature> = listOf(
        // ====================================================================
        // 1. LIGHT & SOUND (Flashlight, Screen Light, Sound Meter, Tone, Noise, Tuner, Metronome, Morse)
        // ====================================================================
        Feature(
            id = "flashlight_normal",
            toolId = "flashlight",
            modeId = "normal",
            title = "Flashlight Normal",
            shortBenefit = "Instant bright LED torch with lock-button toggle",
            icon = Icons.Outlined.FlashlightOn,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.FLASH),
            aliases = listOf("torch", "light", "led", "lamp", "flash")
        ),
        Feature(
            id = "flashlight_brightness",
            toolId = "flashlight",
            modeId = "brightness",
            title = "Flashlight Brightness",
            shortBenefit = "Smooth hardware dimmer control from Dim to MAX on supported LEDs",
            icon = Icons.Outlined.BrightnessHigh,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.FLASH),
            aliases = listOf("dimmer", "torch level", "led intensity", "brightness"),
            premiumFeature = PremiumFeature.FLASHLIGHT_HIGH
        ),
        Feature(
            id = "flashlight_strobe",
            toolId = "flashlight",
            modeId = "strobe",
            title = "Flashlight Strobe",
            shortBenefit = "Adjustable 1–20 Hz tactical & party strobe flasher",
            icon = Icons.Outlined.Bolt,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.FLASH),
            aliases = listOf("blinker", "flicker", "hz flash", "party light"),
            premiumFeature = PremiumFeature.FLASHLIGHT_HIGH
        ),
        Feature(
            id = "flashlight_sos",
            toolId = "flashlight",
            modeId = "sos",
            title = "Flashlight SOS",
            shortBenefit = "International Morse distress beacon signal",
            icon = Icons.Outlined.Sos,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.FLASH),
            aliases = listOf("emergency light", "rescue", "distress", "help signal")
        ),
        Feature(
            id = "flashlight_morse",
            toolId = "flashlight",
            modeId = "morse",
            title = "Flashlight Morse",
            shortBenefit = "Type any message to flash in real-time Morse code",
            icon = Icons.Outlined.Sensors,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.FLASH),
            aliases = listOf("morse beam", "signal light", "telegraph flash"),
            premiumFeature = PremiumFeature.FLASHLIGHT_HIGH
        ),
        Feature(
            id = "screen_light",
            toolId = "flashlight",
            modeId = "screen",
            title = "Screen Light",
            shortBenefit = "Full-brightness soft white display reading lamp",
            icon = Icons.Outlined.LightMode,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = false,
            aliases = listOf("display torch", "soft light", "night light", "front flash")
        ),
        Feature(
            id = "screen_colors",
            toolId = "flashlight",
            modeId = "screen_colors",
            title = "Screen Colors",
            shortBenefit = "Full RGB color wheel & ambient mood screen light",
            icon = Icons.Outlined.Palette,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            aliases = listOf("color lamp", "red night light", "rgb screen", "mood light"),
            premiumFeature = PremiumFeature.FLASHLIGHT_HIGH
        ),
        Feature(
            id = "flashlight_timer",
            toolId = "flashlight",
            modeId = "timer",
            title = "Flashlight Timer",
            shortBenefit = "Auto shut-off sleep timer for LED & screen light",
            icon = Icons.Outlined.Timer,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = false,
            aliases = listOf("auto off torch", "sleep light", "timed flashlight")
        ),
        Feature(
            id = "sound_meter_db",
            toolId = "sound_meter",
            modeId = "meter",
            title = "Sound Meter dB",
            shortBenefit = "Live decibel gauge with OSHA noise safety zones",
            icon = Icons.Outlined.GraphicEq,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            aliases = listOf("decibel", "spl", "noise meter", "loudness", "db meter"),
            premiumFeature = PremiumFeature.SOUND_METER
        ),
        Feature(
            id = "sound_meter_spectrum",
            toolId = "sound_meter",
            modeId = "spectrum",
            title = "Sound Spectrum Analyzer",
            shortBenefit = "Real-time FFT octave band frequency visualizer",
            icon = Icons.Outlined.Equalizer,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            aliases = listOf("fft", "audio spectrum", "frequency bars", "rta"),
            premiumFeature = PremiumFeature.SOUND_METER
        ),
        Feature(
            id = "sound_meter_csv",
            toolId = "sound_meter",
            modeId = "log",
            title = "Sound Peak & CSV Log",
            shortBenefit = "Track min/avg/max dB history & export CSV logs",
            icon = Icons.Outlined.Analytics,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            aliases = listOf("noise log", "db export", "peak decibel"),
            premiumFeature = PremiumFeature.SOUND_METER
        ),
        Feature(
            id = "tone_generator_basic",
            toolId = "tone_generator",
            modeId = "tone",
            title = "Frequency Tone Generator",
            shortBenefit = "Pure sine wave oscillator from 20 Hz to 20,000 Hz",
            icon = Icons.Outlined.Waves,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = false,
            aliases = listOf("sine wave", "hz generator", "audio oscillator", "pitch")
        ),
        Feature(
            id = "tone_generator_waveforms",
            toolId = "tone_generator",
            modeId = "sweep",
            title = "Waveforms & L/R Sweep",
            shortBenefit = "Square, triangle, sawtooth waves & stereo pan sweeps",
            icon = Icons.Outlined.MultilineChart,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            aliases = listOf("square wave", "sawtooth", "stereo sweep", "subwoofer test"),
            premiumFeature = PremiumFeature.TONE_GENERATOR_PRO
        ),
        Feature(
            id = "noise_sleep_mixer",
            toolId = "noise_generator",
            modeId = "mixer",
            title = "Noise & Sleep Mixer",
            shortBenefit = "Mix white, pink, brown noise with rain & campfires",
            icon = Icons.Outlined.Nightlight,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            aliases = listOf("white noise", "brown noise", "pink noise", "sleep sounds", "focus"),
            premiumFeature = PremiumFeature.NOISE_SLEEP_MIXER
        ),
        Feature(
            id = "noise_sleep_timer",
            toolId = "noise_generator",
            modeId = "timer",
            title = "Sleep Fade-Out Timer",
            shortBenefit = "Gentle volume fade-out timer for bedtime ambient mixes",
            icon = Icons.Outlined.Bedtime,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            aliases = listOf("bedtime timer", "ambient fade", "sleep shutoff"),
            premiumFeature = PremiumFeature.NOISE_SLEEP_MIXER
        ),
        Feature(
            id = "tuner_chromatic",
            toolId = "tuner",
            modeId = "chromatic",
            title = "Chromatic Tuner",
            shortBenefit = "Precision cent needle pitch detector (A4 = 440 Hz)",
            icon = Icons.Outlined.MusicNote,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            aliases = listOf("pitch detector", "instrument tuner", "violin tuner", "432hz"),
            premiumFeature = PremiumFeature.TUNER
        ),
        Feature(
            id = "tuner_guitar_ukulele",
            toolId = "tuner",
            modeId = "guitar",
            title = "Guitar & Ukulele Tuner",
            shortBenefit = "String-by-string tuning presets for Guitar, Bass & Uke",
            icon = Icons.Outlined.QueueMusic,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            aliases = listOf("guitar strings", "drop d", "bass tuner", "ukulele"),
            premiumFeature = PremiumFeature.TUNER
        ),
        Feature(
            id = "metronome_beat",
            toolId = "metronome",
            modeId = "beat",
            title = "Metronome",
            shortBenefit = "Visual pendulum beat engine from 30 to 240 BPM",
            icon = Icons.Outlined.AvTimer,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = false,
            aliases = listOf("bpm", "tempo click", "rhythm", "time signature")
        ),
        Feature(
            id = "metronome_tap_tempo",
            toolId = "metronome",
            modeId = "tap",
            title = "Tap Tempo Finder",
            shortBenefit = "Tap along to any song to measure exact BPM",
            icon = Icons.Outlined.TouchApp,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = false,
            aliases = listOf("bpm counter", "beat finder", "song tempo")
        ),
        Feature(
            id = "morse_code_translator",
            toolId = "morse_code",
            modeId = "translate",
            title = "Morse Translator & Beep",
            shortBenefit = "Convert text to Morse dots/dashes with audio & flash",
            icon = Icons.Outlined.RecordVoiceOver,
            category = ToolCategory.LIGHT_AND_SOUND,
            isPremium = false,
            aliases = listOf("cw", "telegraph", "dots and dashes", "morse audio")
        ),

        // ====================================================================
        // 2. PHONE CARE & AUDIO (Speaker Cleaner, Speaker Test, Frequency, Musical, Mic, Vibration, Touch, Battery, Pixels, Device Info)
        // ====================================================================
        Feature(
            id = "speaker_cleaner_water",
            toolId = "speaker_cleaner",
            modeId = "water",
            title = "Speaker Cleaner Water",
            shortBenefit = "165 Hz resonant acoustic wave to eject trapped water",
            icon = Icons.Outlined.WaterDrop,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("water eject", "wet speaker", "fix muffled sound")
        ),
        Feature(
            id = "speaker_cleaner_dust",
            toolId = "speaker_cleaner",
            modeId = "dust",
            title = "Speaker Cleaner Dust",
            shortBenefit = "Pulsed vibration bursts to shake loose speaker dust",
            icon = Icons.Outlined.CleaningServices,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("dust removal", "clean grille", "earpiece cleaner")
        ),
        Feature(
            id = "speaker_test_stereo",
            toolId = "speaker_test",
            modeId = "stereo",
            title = "Speaker Stereo Check",
            shortBenefit = "Verify Left, Right & dual-speaker channel separation",
            icon = Icons.Outlined.SpeakerGroup,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("left right test", "earbuds test", "headphones check")
        ),
        Feature(
            id = "speaker_test_bands",
            toolId = "speaker_test",
            modeId = "bands",
            title = "Speaker Bass & Treble",
            shortBenefit = "Test sub-bass, midrange & high-treble clarity",
            icon = Icons.Outlined.Speaker,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("subwoofer test", "treble test", "distortion check")
        ),
        Feature(
            id = "speaker_test_phase",
            toolId = "speaker_test",
            modeId = "phase",
            title = "Speaker Phase Check",
            shortBenefit = "In-phase vs out-of-phase acoustic polarity test",
            icon = Icons.Outlined.SwapHoriz,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("polarity test", "audio phase", "wiring check")
        ),
        Feature(
            id = "frequency_sweep",
            toolId = "frequency_generator",
            modeId = "sweep",
            title = "Frequency Sweep (Pro)",
            shortBenefit = "Automated logarithmic & linear frequency sweeps",
            icon = Icons.Outlined.Timeline,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = true,
            aliases = listOf("log sweep", "burn in", "room resonance"),
            premiumFeature = PremiumFeature.FREQUENCY_PRO
        ),
        Feature(
            id = "frequency_pulse",
            toolId = "frequency_generator",
            modeId = "pulse",
            title = "Frequency Pulse & Burst",
            shortBenefit = "Configurable duty-cycle tone pulses and acoustic bursts",
            icon = Icons.Outlined.Bolt,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = true,
            aliases = listOf("tone burst", "pulsed hz", "staccato tone"),
            premiumFeature = PremiumFeature.FREQUENCY_PRO
        ),
        Feature(
            id = "frequency_presets",
            toolId = "frequency_presets",
            modeId = "presets",
            title = "Frequency Presets",
            shortBenefit = "One-tap Solfeggio, Mosquito & Dog whistle frequencies",
            icon = Icons.Outlined.Tune,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("432 hz", "528 hz", "dog whistle", "mosquito tone")
        ),
        Feature(
            id = "sound_packs_pro",
            toolId = "frequency_presets",
            modeId = "packs",
            title = "Binaural & Sound Packs",
            shortBenefit = "Curated binaural beats, chakra & acoustic test packs",
            icon = Icons.Outlined.LibraryMusic,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = true,
            aliases = listOf("binaural beats", "alpha waves", "theta waves"),
            premiumFeature = PremiumFeature.FREQUENCY_PRO
        ),
        Feature(
            id = "musical_tones",
            toolId = "musical_tones",
            modeId = "piano",
            title = "Musical Reference Tones",
            shortBenefit = "Playable 12-note octave keyboard with concert pitches",
            icon = Icons.Outlined.Piano,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("piano keys", "note reference", "octave pitch")
        ),
        Feature(
            id = "mic_test_waveform",
            toolId = "mic_test",
            modeId = "waveform",
            title = "Mic Waveform & Clipping",
            shortBenefit = "Live microphone gain bar, peak & clipping detector",
            icon = Icons.Outlined.Mic,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            aliases = listOf("microphone check", "input level", "audio input")
        ),
        Feature(
            id = "mic_test_loopback",
            toolId = "mic_test",
            modeId = "loopback",
            title = "Mic 3s Voice Check",
            shortBenefit = "Record a 3-second sample and play it back instantly",
            icon = Icons.Outlined.Replay,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            aliases = listOf("voice check", "mic playback", "echo test")
        ),
        Feature(
            id = "vibration_test_patterns",
            toolId = "vibration_test",
            modeId = "patterns",
            title = "Vibration Motor Test",
            shortBenefit = "Test haptic actuator with pulse, heartbeat & SOS",
            icon = Icons.Outlined.Vibration,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.VIBRATOR),
            aliases = listOf("haptics check", "rumble test", "motor test")
        ),
        Feature(
            id = "touch_screen_grid",
            toolId = "touch_screen_test",
            modeId = "grid",
            title = "Touch Screen Grid Test",
            shortBenefit = "Swipe every perimeter & center cell to find dead zones",
            icon = Icons.Outlined.GridOn,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("digitizer test", "dead touch zone", "screen swipe")
        ),
        Feature(
            id = "multi_touch_test",
            toolId = "touch_screen_test",
            modeId = "multitouch",
            title = "Multi-Touch Finger Test",
            shortBenefit = "Track up to 10 simultaneous fingers with coordinates",
            icon = Icons.Outlined.PanToolAlt,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("10 point touch", "finger tracker", "pinch test")
        ),
        Feature(
            id = "battery_info_health",
            toolId = "battery_info",
            modeId = "status",
            title = "Battery Health & Voltage",
            shortBenefit = "Live mV voltage, temperature °C/°F & charging rate",
            icon = Icons.Outlined.BatteryChargingFull,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("battery temperature", "charge speed", "power health")
        ),
        Feature(
            id = "screen_pixel_test",
            toolId = "screen_pixel_test",
            modeId = "solid",
            title = "Dead Pixel Inspector",
            shortBenefit = "Full-screen RGBW & black color cycles to spot stuck pixels",
            icon = Icons.Outlined.ScreenshotMonitor,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("stuck pixel", "lcd test", "oled burn in")
        ),
        Feature(
            id = "device_info_sensors",
            toolId = "device_info",
            modeId = "sensors",
            title = "Device & Sensor Diagnostics",
            shortBenefit = "Inspect hardware SoC, RAM, display & all onboard sensors",
            icon = Icons.Outlined.PermDeviceInformation,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = false,
            aliases = listOf("hardware specs", "sensor list", "cpu ram info")
        ),
        Feature(
            id = "home_screen_widgets",
            toolId = "home_widgets",
            modeId = null,
            title = "Home Screen Widgets",
            shortBenefit = "Pin Glass Flashlight, Compass, Solunar, Scan & Weather widgets",
            icon = Icons.Outlined.Widgets,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = true,
            aliases = listOf("glance widget", "launcher widget", "desktop widget"),
            premiumFeature = PremiumFeature.HOME_SCREEN_WIDGETS
        ),
        Feature(
            id = "custom_style_studio",
            toolId = "custom_style_studio",
            modeId = null,
            title = "Style & Accent Studio",
            shortBenefit = "Custom hex accent color picker, tile styles & hero layouts",
            icon = Icons.Outlined.ColorLens,
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            isPremium = true,
            aliases = listOf("accent color", "tile style", "theme builder"),
            premiumFeature = PremiumFeature.CUSTOMIZATION_PRO
        ),

        // ====================================================================
        // 3. MEASURE & SENSORS (Level, Ruler, Protractor, Magnifier, Color Picker, Lux, Metal, Compass, Speedometer, Altimeter, Vibration Meter, Scanner)
        // ====================================================================
        Feature(
            id = "level_bubble",
            toolId = "bubble_level",
            modeId = "bubble",
            title = "Level Bubble",
            shortBenefit = "Classic 2-axis bullseye & tube carpenter spirit level",
            icon = Icons.Outlined.Explore,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            aliases = listOf("spirit level", "carpenter level", "bullseye")
        ),
        Feature(
            id = "level_surface",
            toolId = "bubble_level",
            modeId = "surface",
            title = "Level Surface",
            shortBenefit = "Flat table & countertop X/Y plane tilt balance",
            icon = Icons.Outlined.CropSquare,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            aliases = listOf("table level", "flatness", "xy level")
        ),
        Feature(
            id = "level_plumb",
            toolId = "bubble_level",
            modeId = "plumb",
            title = "Level Plumb",
            shortBenefit = "Vertical wall & post alignment plumb bob reference",
            icon = Icons.Outlined.Height,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            aliases = listOf("plumb bob", "vertical wall", "post level")
        ),
        Feature(
            id = "level_angle",
            toolId = "clinometer",
            modeId = "angle",
            title = "Level Angle (Clinometer)",
            shortBenefit = "Precision slope in degrees, % grade, roof pitch &音声 lock",
            icon = Icons.Outlined.Architecture,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            aliases = listOf("inclinometer", "slope meter", "roof pitch", "grade %"),
            premiumFeature = PremiumFeature.ANGLE
        ),
        Feature(
            id = "level_profiles",
            toolId = "bubble_level",
            modeId = "profiles",
            title = "Level Calibration Profiles",
            shortBenefit = "Save custom zero-tare offsets for Desk, Car & Tripod",
            icon = Icons.Outlined.Tune,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            aliases = listOf("tare level", "zero offset", "calibration memory"),
            premiumFeature = PremiumFeature.LEVEL_CALIBRATION_PROFILES
        ),
        Feature(
            id = "measurement_history_csv",
            toolId = "clinometer",
            modeId = "history",
            title = "Angle & Level History CSV",
            shortBenefit = "Log 500 angle/level measurements with notes & CSV export",
            icon = Icons.Outlined.TableView,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            aliases = listOf("measurement log", "export angles", "clinometer csv"),
            premiumFeature = PremiumFeature.MEASUREMENT_HISTORY_CSV
        ),
        Feature(
            id = "screen_ruler",
            toolId = "ruler",
            modeId = "ruler",
            title = "On-Screen Ruler (cm / in)",
            shortBenefit = "DPI-calibrated millimeter & fractional inch edge ruler",
            icon = Icons.Outlined.Straighten,
            category = ToolCategory.MEASURE,
            isPremium = false,
            aliases = listOf("measure length", "millimeters", "inches", "tape measure")
        ),
        Feature(
            id = "screen_caliper",
            toolId = "ruler",
            modeId = "caliper",
            title = "Dual-Jaw Caliper",
            shortBenefit = "Slide two precision touch jaws to measure small objects",
            icon = Icons.Outlined.CompareArrows,
            category = ToolCategory.MEASURE,
            isPremium = false,
            aliases = listOf("vernier", "thickness measure", "coin size")
        ),
        Feature(
            id = "protractor_arc",
            toolId = "protractor",
            modeId = "arc",
            title = "Touch Protractor",
            shortBenefit = "180° interactive dual-ray angle & complement finder",
            icon = Icons.Outlined.RoundedCorner,
            category = ToolCategory.MEASURE,
            isPremium = false,
            aliases = listOf("angle finder", "degree arc", "geometry")
        ),
        Feature(
            id = "magnifier_glass",
            toolId = "magnifier",
            modeId = "zoom",
            title = "Magnifier & Freeze Frame",
            shortBenefit = "Up to 8x macro camera zoom with torch & frame freeze",
            icon = Icons.Outlined.ZoomIn,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.CAMERA),
            aliases = listOf("magnifying glass", "reading lens", "macro zoom")
        ),
        Feature(
            id = "color_picker_camera",
            toolId = "color_picker",
            modeId = "live",
            title = "Camera Color Picker",
            shortBenefit = "Sample live HEX, RGB, HSL & CMYK colors from camera",
            icon = Icons.Outlined.Colorize,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.CAMERA),
            aliases = listOf("eyedropper", "hex finder", "paint matcher", "rgb")
        ),
        Feature(
            id = "lux_light_meter",
            toolId = "light_meter",
            modeId = "lux",
            title = "Lux & Foot-Candle Meter",
            shortBenefit = "Measure ambient illumination for plants, offices & studios",
            icon = Icons.Outlined.Lightbulb,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.LIGHT),
            aliases = listOf("lumen", "foot candles", "plant light", "brightness sensor")
        ),
        Feature(
            id = "photo_light_meter_ev",
            toolId = "light_meter",
            modeId = "photo_ev",
            title = "Photo Light Meter (EV)",
            shortBenefit = "Calculate Exposure Value, ISO, Shutter Speed & Aperture f-stop",
            icon = Icons.Outlined.Camera,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.LIGHT),
            aliases = listOf("exposure meter", "shutter speed", "f stop", "iso calculator"),
            premiumFeature = PremiumFeature.PHOTO_LIGHT_METER
        ),
        Feature(
            id = "metal_detector_emf",
            toolId = "metal_detector",
            modeId = "gauge",
            title = "Metal & EMF Detector",
            shortBenefit = "3-axis microtesla (µT) magnetic field & stud finder",
            icon = Icons.Outlined.Radar,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.MAGNETOMETER),
            aliases = listOf("stud finder", "magnetometer", "emf meter", "ferromagnetic"),
            premiumFeature = PremiumFeature.METAL_DETECTOR
        ),
        Feature(
            id = "compass_main",
            toolId = "compass",
            modeId = "compass",
            title = "Compass",
            shortBenefit = "Sensor-fused magnetic & true-north azimuth rose",
            icon = Icons.Outlined.Explore,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.MAGNETOMETER),
            aliases = listOf("north", "bearing", "heading", "azimuth", "navigation")
        ),
        Feature(
            id = "compass_qibla",
            toolId = "compass",
            modeId = "qibla",
            title = "Qibla Finder",
            shortBenefit = "Great-circle bearing & distance to the Kaaba in Makkah",
            icon = Icons.Outlined.Mosque,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.MAGNETOMETER),
            aliases = listOf("mecca direction", "prayer compass", "kaaba bearing")
        ),
        Feature(
            id = "compass_waypoints",
            toolId = "compass",
            modeId = "waypoints",
            title = "Waypoints",
            shortBenefit = "Save GPS targets with live bearing arrow & distance",
            icon = Icons.Outlined.Flag,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            aliases = listOf("gps pin", "camp marker", "target bearing")
        ),
        Feature(
            id = "compass_waypoints_unlimited",
            toolId = "compass",
            modeId = "waypoints_pro",
            title = "Unlimited Waypoints & GPX",
            shortBenefit = "Save unlimited GPS waypoints with coordinates export",
            icon = Icons.Outlined.AddLocationAlt,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            aliases = listOf("gpx export", "unlimited pins", "backcountry waypoints"),
            premiumFeature = PremiumFeature.UNLIMITED_WAYPOINTS
        ),
        Feature(
            id = "compass_sun_moon_direction",
            toolId = "compass",
            modeId = "sun_moon_direction",
            title = "Compass Sun & Moon Direction",
            shortBenefit = "Live solar & lunar azimuth bearings and elevation on the compass rose",
            icon = Icons.Outlined.WbTwilight,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.MAGNETOMETER),
            aliases = listOf("sun bearing", "moon direction", "solar compass", "sun & moon direction"),
            premiumFeature = PremiumFeature.SUN_MOON_DOME
        ),
        Feature(
            id = "speedometer_digital",
            toolId = "speedometer",
            modeId = "digital",
            title = "Speedometer Digital",
            shortBenefit = "High-contrast GPS speed in km/h, mph & knots",
            icon = Icons.Outlined.Speed,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            aliases = listOf("gps speed", "kmh", "mph", "knots", "velocity")
        ),
        Feature(
            id = "speedometer_analog",
            toolId = "speedometer",
            modeId = "analog",
            title = "Speedometer Analog",
            shortBenefit = "Precision radial cockpit dial with max & average speed",
            icon = Icons.Outlined.AvTimer,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            aliases = listOf("dial gauge", "dashboard speed", "cockpit gauge")
        ),
        Feature(
            id = "speedometer_hud",
            toolId = "speedometer",
            modeId = "hud",
            title = "Speedometer HUD",
            shortBenefit = "Mirrored windshield head-up projection mode for night driving",
            icon = Icons.Outlined.Flip,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            aliases = listOf("windshield projection", "heads up display", "mirror speed"),
            premiumFeature = PremiumFeature.SPEEDOMETER_HUD_TRIP
        ),
        Feature(
            id = "speedometer_trip_log",
            toolId = "speedometer",
            modeId = "trip",
            title = "Speedometer Trip Log",
            shortBenefit = "Record trip distance, moving time, speed graph & CSV",
            icon = Icons.Outlined.Route,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            aliases = listOf("odometer", "trip meter", "drive log", "mileage tracker"),
            premiumFeature = PremiumFeature.SPEEDOMETER_HUD_TRIP
        ),
        Feature(
            id = "altimeter_barometer",
            toolId = "altimeter",
            modeId = "altitude",
            title = "Altimeter & Barometer",
            shortBenefit = "Barometric + GPS elevation, QNH sea-level pressure & trend",
            icon = Icons.Outlined.Terrain,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            aliases = listOf("elevation", "mountain height", "hpa", "pressure sensor"),
            premiumFeature = PremiumFeature.ALTIMETER
        ),
        Feature(
            id = "vibration_gforce_meter",
            toolId = "vibration_meter",
            modeId = "seismograph",
            title = "Vibration & G-Force Meter",
            shortBenefit = "Real-time 3-axis seismograph & Modified Mercalli scale",
            icon = Icons.Outlined.Sensors,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            aliases = listOf("seismometer", "earthquake", "g force", "accelerometer graph"),
            premiumFeature = PremiumFeature.VIBRATION_GFORCE
        ),
        Feature(
            id = "scanner_qr",
            toolId = "open_qr_scanner",
            modeId = "qr",
            title = "Scanner QR",
            shortBenefit = "Instant camera & gallery QR reader for URLs, Wi-Fi & contacts",
            icon = Icons.Outlined.QrCodeScanner,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.CAMERA),
            aliases = listOf("qr reader", "scan wifi qr", "camera scan")
        ),
        Feature(
            id = "scanner_barcode",
            toolId = "open_qr_scanner",
            modeId = "barcode",
            title = "Scanner Barcode",
            shortBenefit = "Read UPC, EAN, Code-128, ISBN & product barcodes",
            icon = Icons.Outlined.ViewWeek,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.CAMERA),
            aliases = listOf("upc reader", "ean scanner", "product code", "isbn")
        ),
        Feature(
            id = "scanner_document",
            toolId = "open_doc_scanner",
            modeId = "document",
            title = "Scanner Document",
            shortBenefit = "Auto-crop edge detection, perspective fix & B&W filters",
            icon = Icons.Outlined.DocumentScanner,
            category = ToolCategory.MEASURE,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.CAMERA),
            aliases = listOf("scan to pdf", "receipt scanner", "paper scan", "camscanner")
        ),
        Feature(
            id = "scanner_id_card",
            toolId = "open_doc_scanner",
            modeId = "id_card",
            title = "Scanner ID Card (2-Sided)",
            shortBenefit = "Scan front & back of ID or driver license onto one page",
            icon = Icons.Outlined.Badge,
            category = ToolCategory.MEASURE,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.CAMERA),
            aliases = listOf("id scan", "license front back", "card copy"),
            premiumFeature = PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE
        ),

        // ====================================================================
        // 4. MEDIA STUDIO (Photo Converter, Background Remover, Photo Editor, Collage, Passport, PDF Tools, Audio, Video, EXIF, Image Toolkit)
        // ====================================================================
        Feature(
            id = "photo_converter_single",
            toolId = "photo_converter",
            modeId = "convert",
            title = "Photo Converter",
            shortBenefit = "Convert HEIC, WebP, PNG, BMP, AVIF & JPG with quality live preview",
            icon = Icons.Outlined.Transform,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("heic to jpg", "webp to png", "image format", "avif converter")
        ),
        Feature(
            id = "photo_converter_batch",
            toolId = "photo_converter",
            modeId = "batch",
            title = "Batch Photo Converter (50)",
            shortBenefit = "Convert & resize up to 50 full-res photos at once with custom naming",
            icon = Icons.Outlined.BurstMode,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("bulk heic", "batch image convert", "4k resize"),
            premiumFeature = PremiumFeature.PHOTO_CONVERTER_PRO
        ),
        Feature(
            id = "bg_remover_cutout",
            toolId = "background_remover",
            modeId = "cutout",
            title = "Background Remover",
            shortBenefit = "On-device AI subject cutout to transparent PNG or solid backdrop",
            icon = Icons.Outlined.AutoFixHigh,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("cutout", "transparent background", "erase background", "png sticker")
        ),
        Feature(
            id = "bg_remover_hd",
            toolId = "background_remover",
            modeId = "hd",
            title = "Background Remover HD",
            shortBenefit = "Full-resolution cutouts, custom photo backdrops & drop shadows",
            icon = Icons.Outlined.HighQuality,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("hd cutout", "product shadow", "custom background"),
            premiumFeature = PremiumFeature.BACKGROUND_REMOVER_PRO
        ),
        Feature(
            id = "photo_editor_adjust",
            toolId = "photo_editor",
            modeId = "adjust",
            title = "Photo Editor Crop & Adjust",
            shortBenefit = "Crop, straighten, exposure, contrast, warmth, vignette & sharpen",
            icon = Icons.Outlined.Tune,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("crop photo", "rotate image", "brightness contrast", "retouch")
        ),
        Feature(
            id = "photo_editor_filters",
            toolId = "photo_editor",
            modeId = "filters",
            title = "Photo Editor 20 Filters",
            shortBenefit = "20 studio color LUTs, vintage film, B&W & cinematic looks",
            icon = Icons.Outlined.FilterVintage,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("photo filters", "film look", "black and white", "sepia")
        ),
        Feature(
            id = "photo_editor_annotate",
            toolId = "photo_editor",
            modeId = "annotate",
            title = "Photo Markup & Privacy Blur",
            shortBenefit = "Draw arrows, add styled text, stickers & mosaic pixel blur",
            icon = Icons.Outlined.Brush,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("blur face", "censor photo", "meme maker", "draw on photo")
        ),
        Feature(
            id = "photo_editor_curves_pro",
            toolId = "photo_editor",
            modeId = "pro",
            title = "Photo Editor Curves & Grain",
            shortBenefit = "RGB tone curves, film grain, 10+ Pro LUTs & 4K export",
            icon = Icons.Outlined.AutoGraph,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("rgb curves", "film grain", "color grading", "pro filters"),
            premiumFeature = PremiumFeature.PHOTO_EDITOR_PRO
        ),
        Feature(
            id = "photo_collage_maker",
            toolId = "photo_editor",
            modeId = "collage",
            title = "Photo Collage (2–6 Grids)",
            shortBenefit = "Combine 2 to 6 photos into customizable grid layouts & borders",
            icon = Icons.Outlined.DashboardCustomize,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("photo grid", "side by side photo", "frame layout"),
            premiumFeature = PremiumFeature.PHOTO_EDITOR_PRO
        ),
        Feature(
            id = "passport_id_photo",
            toolId = "passport_photo",
            modeId = "single",
            title = "Passport & Visa ID Photo",
            shortBenefit = "US 2x2\", UK/EU 35x45mm, India & custom biometric sizes",
            icon = Icons.Outlined.AssignmentInd,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("2x2 photo", "35x45mm", "visa photo", "biometric id"),
            premiumFeature = PremiumFeature.PASSPORT_ID_PHOTO
        ),
        Feature(
            id = "passport_print_sheet",
            toolId = "passport_photo",
            modeId = "sheet",
            title = "Passport 4x6\" Print Sheet",
            shortBenefit = "Tile multiple ID photos onto a printable 4x6\" or A4 sheet",
            icon = Icons.Outlined.Print,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("4x6 print", "id photo sheet", "cut guides"),
            premiumFeature = PremiumFeature.PASSPORT_ID_PHOTO
        ),
        Feature(
            id = "pdf_images_to_pdf",
            toolId = "pdf_tools",
            modeId = "img2pdf",
            title = "Images to PDF",
            shortBenefit = "Convert photos and receipts into clean A4 or Letter PDFs",
            icon = Icons.Outlined.PictureAsPdf,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("jpg to pdf", "photos to pdf", "create pdf")
        ),
        Feature(
            id = "pdf_to_images",
            toolId = "pdf_tools",
            modeId = "pdf2img",
            title = "PDF to JPG / PNG",
            shortBenefit = "Render PDF pages as crisp high-DPI images to your gallery",
            icon = Icons.Outlined.Collections,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("pdf to jpg", "pdf to png", "extract pdf pages")
        ),
        Feature(
            id = "pdf_merge",
            toolId = "pdf_tools",
            modeId = "merge",
            title = "Merge PDF",
            shortBenefit = "Combine multiple PDF documents into a single ordered file",
            icon = Icons.Outlined.MergeType,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("join pdf", "combine pdf", "binder")
        ),
        Feature(
            id = "pdf_split",
            toolId = "pdf_tools",
            modeId = "split",
            title = "Split & Reorder PDF",
            shortBenefit = "Extract page ranges, rotate or delete unwanted PDF pages",
            icon = Icons.Outlined.CallSplit,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("extract pages", "remove pdf page", "rotate pdf")
        ),
        Feature(
            id = "pdf_sign",
            toolId = "pdf_tools",
            modeId = "sign",
            title = "Sign PDF",
            shortBenefit = "Draw your handwritten signature and stamp it onto any page",
            icon = Icons.Outlined.Draw,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("e-sign", "signature stamp", "autograph pdf")
        ),
        Feature(
            id = "pdf_compress",
            toolId = "pdf_tools",
            modeId = "compress",
            title = "Compress PDF",
            shortBenefit = "Shrink large PDF file sizes for email & portal uploads",
            icon = Icons.Outlined.Compress,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("reduce pdf size", "shrink pdf", "small pdf"),
            premiumFeature = PremiumFeature.PDF_IMAGE_PRO
        ),
        Feature(
            id = "pdf_password_lock",
            toolId = "pdf_tools",
            modeId = "security",
            title = "Password Lock & Unlock PDF",
            shortBenefit = "Protect confidential PDFs or strip known open passwords",
            icon = Icons.Outlined.Lock,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("encrypt pdf", "unlock pdf", "password protect"),
            premiumFeature = PremiumFeature.PDF_IMAGE_PRO
        ),
        Feature(
            id = "pdf_watermark_numbers",
            toolId = "pdf_tools",
            modeId = "watermark",
            title = "PDF Watermark & Page Numbers",
            shortBenefit = "Stamp angled text watermarks and automatic page footers",
            icon = Icons.Outlined.BrandingWatermark,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("confidential stamp", "number pdf pages", "header footer"),
            premiumFeature = PremiumFeature.PDF_IMAGE_PRO
        ),
        Feature(
            id = "pdf_redact",
            toolId = "pdf_tools",
            modeId = "redact",
            title = "Redact PDF",
            shortBenefit = "Permanently black out sensitive text & flatten pages",
            icon = Icons.Outlined.VisibilityOff,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("black out pdf", "sanitize pdf", "remove ssn"),
            premiumFeature = PremiumFeature.PDF_IMAGE_PRO
        ),
        Feature(
            id = "audio_converter",
            toolId = "audio_converter",
            modeId = "convert",
            title = "Audio Converter",
            shortBenefit = "Convert WAV, M4A/AAC, OGG, FLAC with bitrate & mono/stereo",
            icon = Icons.Outlined.AudioFile,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("m4a to wav", "audio bitrate", "sample rate")
        ),
        Feature(
            id = "audio_cutter_ringtone",
            toolId = "audio_converter",
            modeId = "trim",
            title = "Audio Cutter & Ringtone",
            shortBenefit = "Trim waveform clips with fade-in/out & normalize gain",
            icon = Icons.Outlined.ContentCut,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("ringtone maker", "trim audio", "fade in out")
        ),
        Feature(
            id = "audio_merge_speed_pro",
            toolId = "audio_converter",
            modeId = "merge",
            title = "Audio Merge, Speed & Pitch",
            shortBenefit = "Join multiple audio tracks, adjust tempo & batch convert",
            icon = Icons.Outlined.QueueMusic,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("join audio", "pitch shift", "speed up audio"),
            premiumFeature = PremiumFeature.AUDIO_CONVERTER_PRO
        ),
        Feature(
            id = "video_trim_mute",
            toolId = "video_tools",
            modeId = "trim",
            title = "Video Trim & Mute",
            shortBenefit = "Cut start/end points or strip audio tracks without quality loss",
            icon = Icons.Outlined.VideoFile,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("cut video", "remove video sound", "mute clip")
        ),
        Feature(
            id = "video_extract_audio",
            toolId = "video_tools",
            modeId = "extract_audio",
            title = "Video to Audio Extractor",
            shortBenefit = "Rip the audio track from any MP4/MOV video to M4A or WAV",
            icon = Icons.Outlined.SpatialAudioOff,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("mp4 to m4a", "extract sound", "video audio rip")
        ),
        Feature(
            id = "video_compress_pro",
            toolId = "video_tools",
            modeId = "compress",
            title = "Video Compress & Resize",
            shortBenefit = "Shrink video size to 1080p/720p/480p for messaging apps",
            icon = Icons.Outlined.VideoSettings,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("shrink video", "downscale 4k", "whatsapp video size"),
            premiumFeature = PremiumFeature.VIDEO_COMPRESS_PRO
        ),
        Feature(
            id = "exif_viewer_clean",
            toolId = "exif_viewer",
            modeId = "inspect",
            title = "EXIF Viewer & Privacy Scrubber",
            shortBenefit = "Inspect camera shutter/GPS tags and save a clean copy",
            icon = Icons.Outlined.Policy,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("remove gps from photo", "photo metadata", "strip exif")
        ),
        Feature(
            id = "image_palette_extractor",
            toolId = "image_toolkit",
            modeId = "palette",
            title = "Photo Color Palette",
            shortBenefit = "Extract 6 dominant HEX & RGB swatches from any photo",
            icon = Icons.Outlined.Palette,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("color swatches", "image hex codes", "theme from photo")
        ),
        Feature(
            id = "image_base64_codec",
            toolId = "image_toolkit",
            modeId = "base64",
            title = "Image Base64 Codec",
            shortBenefit = "Encode images to data-URI strings or decode Base64 to PNG",
            icon = Icons.Outlined.DataObject,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = false,
            aliases = listOf("data uri", "base64 to image", "html image string")
        ),
        Feature(
            id = "image_bulk_compress",
            toolId = "image_toolkit",
            modeId = "bulk",
            title = "Bulk Image Compress",
            shortBenefit = "Compress up to 10 gallery photos simultaneously",
            icon = Icons.Outlined.PhotoSizeSelectLarge,
            category = ToolCategory.MEDIA_STUDIO,
            isPremium = true,
            aliases = listOf("batch compress", "optimize images"),
            premiumFeature = PremiumFeature.IMAGE_TOOLKIT
        ),

        // ====================================================================
        // 5. TRAVEL, TIME & OUTDOORS (Solunar, Sun & Moon, Weather, Planner, Alerts, Walk/Run, Fishing Log, Trip Checklist, Pedometer, Parking, Clocks, Timers, Counters)
        // ====================================================================
        Feature(
            id = "outdoor_solunar_today",
            toolId = "open_outdoors_solunar",
            modeId = "today",
            title = "Solunar Forecast",
            shortBenefit = "Daily major & minor wildlife feeding windows and rating",
            icon = Icons.Outlined.Waves,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            aliases = listOf("fishing times", "hunting table", "bite times", "major minor")
        ),
        Feature(
            id = "outdoor_solunar_7day",
            toolId = "open_outdoors_solunar",
            modeId = "7day",
            title = "7-Day Solunar Forecast",
            shortBenefit = "Full week Solunar activity ratings and peak window tables",
            icon = Icons.Outlined.CalendarMonth,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("weekly fishing", "solunar week", "best fishing day"),
            premiumFeature = PremiumFeature.SOLUNAR_7DAY
        ),
        Feature(
            id = "outdoor_sun_moon",
            toolId = "open_outdoors_sun_moon",
            modeId = "ephemeris",
            title = "Sun Path & Golden/Blue Hour",
            shortBenefit = "Sunrise, sunset, golden hour, blue hour & lunar illumination",
            icon = Icons.Outlined.WbTwilight,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("golden hour", "blue hour", "moon phase", "sunrise sunset"),
            premiumFeature = PremiumFeature.SUN_MOON_DOME
        ),
        Feature(
            id = "outdoor_sky_dome",
            toolId = "open_outdoors_sun_moon",
            modeId = "dome",
            title = "Interactive Sky Dome",
            shortBenefit = "3D solar & lunar azimuth path scrubber for any hour & date",
            icon = Icons.Outlined.Public,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("sun path", "shadow finder", "solar arc", "sky map"),
            premiumFeature = PremiumFeature.SUN_MOON_DOME
        ),
        Feature(
            id = "outdoor_sun_moon_calendar",
            toolId = "sun_moon_calendar",
            modeId = null,
            title = "Monthly Sun & Moon Calendar",
            shortBenefit = "Monthly lunar phases, full/new moons & top Solunar score days",
            icon = Icons.Outlined.EventNote,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("moon calendar", "lunar month", "full moon dates"),
            premiumFeature = PremiumFeature.SUN_MOON_CALENDAR
        ),
        Feature(
            id = "outdoor_planner_7day",
            toolId = "outdoor_planner",
            modeId = null,
            title = "Outdoor Planner (7-Day)",
            shortBenefit = "Ranks the next 7 days for Fishing, Hunting or Photography",
            icon = Icons.Outlined.Insights,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("best day finder", "photo planner", "fishing rank"),
            premiumFeature = PremiumFeature.OUTDOOR_PLANNER_7DAY
        ),
        Feature(
            id = "outdoor_alerts",
            toolId = "outdoor_alerts",
            modeId = null,
            title = "Solunar & Golden Hour Alerts",
            shortBenefit = "Local notifications before peak Solunar windows, golden hour & rain",
            icon = Icons.Outlined.NotificationsActive,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("fishing reminder", "sunset alarm", "rain alert"),
            premiumFeature = PremiumFeature.OUTDOOR_ALERTS
        ),
        Feature(
            id = "outdoor_weather_current",
            toolId = "open_outdoors_weather",
            modeId = "current",
            title = "Weather & Barometer",
            shortBenefit = "Live local temperature, wind, humidity & pressure trend",
            icon = Icons.Outlined.Cloud,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            aliases = listOf("forecast", "wind speed", "barometric pressure", "rain")
        ),
        Feature(
            id = "outdoor_weather_7day",
            toolId = "open_outdoors_weather",
            modeId = "7day",
            title = "Hourly & 7-Day Weather",
            shortBenefit = "24-hour precipitation chart and extended 7-day forecast",
            icon = Icons.Outlined.WbSunny,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("hourly weather", "weekly forecast", "rain probability"),
            premiumFeature = PremiumFeature.WEATHER_HOURLY_7DAY
        ),
        Feature(
            id = "outdoor_air_quality_uv",
            toolId = "open_outdoors_weather",
            modeId = "aqi_uv",
            title = "Air Quality (AQI) & UV Index",
            shortBenefit = "PM2.5, PM10, Ozone & hourly UV burn-risk protection guide",
            icon = Icons.Outlined.Air,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("aqi", "pm2.5", "uv index", "pollen smog"),
            premiumFeature = PremiumFeature.AIR_QUALITY_UV_PRO
        ),
        Feature(
            id = "walk_run_tracker",
            toolId = "walk_run_tracker",
            modeId = "track",
            title = "Walk & Run GPS Tracker",
            shortBenefit = "Track pace, splits, elevation gain, calories & GPX route",
            icon = Icons.Outlined.DirectionsRun,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            aliases = listOf("jogging", "hiking gps", "pace calculator", "gpx recorder"),
            premiumFeature = PremiumFeature.WALK_RUN_TRACKER
        ),
        Feature(
            id = "fishing_log_catches",
            toolId = "fishing_log",
            modeId = "log",
            title = "Fishing Catch Log",
            shortBenefit = "Record species, weight, bait, weather, pressure & moon phase",
            icon = Icons.Outlined.Phishing,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("angler diary", "catch journal", "lure log"),
            premiumFeature = PremiumFeature.FISHING_LOG
        ),
        Feature(
            id = "trip_gear_checklists",
            toolId = "trip_checklists",
            modeId = "templates",
            title = "Trip & Gear Checklists",
            shortBenefit = "6 outdoor & travel packing templates with pack weight total",
            icon = Icons.Outlined.Backpack,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("camping checklist", "packing list", "ultralight gear"),
            premiumFeature = PremiumFeature.TRIP_GEAR_CHECKLISTS
        ),
        Feature(
            id = "pedometer_daily",
            toolId = "pedometer",
            modeId = "today",
            title = "Pedometer Step Counter",
            shortBenefit = "Zero-GPS hardware step counter with distance & kcal burned",
            icon = Icons.Outlined.DirectionsWalk,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.STEP_COUNTER),
            aliases = listOf("steps", "daily walk", "calorie counter")
        ),
        Feature(
            id = "pedometer_history_pro",
            toolId = "pedometer",
            modeId = "history",
            title = "Pedometer 30-Day History",
            shortBenefit = "30-day step trends, goal streak badges & CSV export",
            icon = Icons.Outlined.EmojiEvents,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            requiredCapabilities = setOf(HardwareCapability.STEP_COUNTER),
            aliases = listOf("step streaks", "monthly steps", "fitness chart"),
            premiumFeature = PremiumFeature.PEDOMETER_HISTORY
        ),
        Feature(
            id = "parking_saver",
            toolId = "parking_saver",
            modeId = "park",
            title = "Parking Location Saver",
            shortBenefit = "One-tap GPS car pin, level/spot note & meter expiry timer",
            icon = Icons.Outlined.LocalParking,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            aliases = listOf("find my car", "parking meter", "where did i park")
        ),
        Feature(
            id = "world_clock_cities",
            toolId = "world_clock",
            modeId = "clocks",
            title = "World Clock",
            shortBenefit = "Track international time zones with day/night indicators",
            icon = Icons.Outlined.Schedule,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            aliases = listOf("time zones", "utc", "gmt", "international time")
        ),
        Feature(
            id = "world_clock_meeting_planner",
            toolId = "world_clock",
            modeId = "planner",
            title = "Time Zone Meeting Slider",
            shortBenefit = "Scrub across unlimited cities to find overlapping work hours",
            icon = Icons.Outlined.Groups,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("meeting planner", "timezone overlap", "unlimited clocks"),
            premiumFeature = PremiumFeature.UNLIMITED_WORLD_CLOCKS
        ),
        Feature(
            id = "stopwatch_laps",
            toolId = "world_clock",
            modeId = "stopwatch",
            title = "Stopwatch & Lap Splits",
            shortBenefit = "Millisecond precision chronograph with fastest/slowest lap flags",
            icon = Icons.Outlined.Timer,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            aliases = listOf("chronograph", "lap timer", "split time")
        ),
        Feature(
            id = "countdown_timer",
            toolId = "world_clock",
            modeId = "timer",
            title = "Countdown Timer",
            shortBenefit = "Quick preset & custom duration countdown with alarm alert",
            icon = Icons.Outlined.HourglassBottom,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            aliases = listOf("kitchen timer", "egg timer", "alarm countdown")
        ),
        Feature(
            id = "hiit_interval_timer",
            toolId = "interval_timer",
            modeId = "hiit",
            title = "HIIT & Tabata Interval Timer",
            shortBenefit = "Custom Warmup, Work, Rest & Cool-down rounds with audio cues",
            icon = Icons.Outlined.FitnessCenter,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("tabata", "workout timer", "circuit training", "boxing rounds"),
            premiumFeature = PremiumFeature.INTERVAL_TIMER
        ),
        Feature(
            id = "pomodoro_focus",
            toolId = "pomodoro",
            modeId = "focus",
            title = "Pomodoro Focus Timer",
            shortBenefit = "25/5 focus & break cycles for distraction-free deep work",
            icon = Icons.Outlined.SelfImprovement,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            aliases = listOf("study timer", "deep work", "25 minute timer")
        ),
        Feature(
            id = "pomodoro_stats_pro",
            toolId = "pomodoro",
            modeId = "stats",
            title = "Pomodoro Stats & Ambient",
            shortBenefit = "Weekly focus charts, custom work/break lengths & white noise",
            icon = Icons.Outlined.QueryStats,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("focus analytics", "study streaks", "productivity log"),
            premiumFeature = PremiumFeature.POMODORO_STATS
        ),
        Feature(
            id = "event_countdown",
            toolId = "event_countdown",
            modeId = "events",
            title = "Event & Milestone Countdown",
            shortBenefit = "Live days/hours/minutes cards for trips, birthdays & launches",
            icon = Icons.Outlined.Celebration,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = true,
            aliases = listOf("days until", "vacation countdown", "anniversary"),
            premiumFeature = PremiumFeature.EVENT_COUNTDOWN
        ),
        Feature(
            id = "tally_counter",
            toolId = "tally_counter",
            modeId = "tally",
            title = "Tally Counter",
            shortBenefit = "Multi-slot clicker with custom step size, target & haptics",
            icon = Icons.Outlined.ControlPointDuplicate,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            aliases = listOf("clicker", "headcount", "lap counter", "inventory count")
        ),
        Feature(
            id = "randomizer_dice_coin",
            toolId = "randomizer",
            modeId = "dice",
            title = "Dice Roller, Coin & Wheel",
            shortBenefit = "3D dice (D4–D20), coin flip, spin wheel & team picker",
            icon = Icons.Outlined.Casino,
            category = ToolCategory.TRAVEL_AND_TIME,
            isPremium = false,
            aliases = listOf("roll dice", "flip a coin", "decision wheel", "random number")
        ),

        // ====================================================================
        // 6. CONVERT & WRITE (Every Converter Category, Every Calculator, Every Note Type, QR Generator, Text & Password Tools)
        // ====================================================================
        // --- Unit Converter Categories ---
        Feature(
            id = "conv_length",
            toolId = "unit_converter",
            modeId = "Length",
            title = "Length Converter",
            shortBenefit = "Convert meters, feet, inches, miles, km, yards & nautical miles",
            icon = Icons.Outlined.Straighten,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("cm to inches", "meters to feet", "km to miles", "distance converter")
        ),
        Feature(
            id = "conv_weight",
            toolId = "unit_converter",
            modeId = "Weight",
            title = "Weight & Mass Converter",
            shortBenefit = "Convert kg, lbs, ounces, grams, stones & metric tons",
            icon = Icons.Outlined.Scale,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("kg to lbs", "grams to oz", "pounds to kilos", "mass")
        ),
        Feature(
            id = "conv_temperature",
            toolId = "unit_converter",
            modeId = "Temperature",
            title = "Temperature Converter",
            shortBenefit = "Instant Celsius (°C), Fahrenheit (°F) & Kelvin (K) conversion",
            icon = Icons.Outlined.Thermostat,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("celsius to fahrenheit", "c to f", "kelvin")
        ),
        Feature(
            id = "conv_volume",
            toolId = "unit_converter",
            modeId = "Volume",
            title = "Volume Converter",
            shortBenefit = "Convert liters, milliliters, US/UK gallons, pints, cups & fl oz",
            icon = Icons.Outlined.LocalDrink,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("liters to gallons", "ml to oz", "liquid measure")
        ),
        Feature(
            id = "conv_area",
            toolId = "unit_converter",
            modeId = "Area",
            title = "Area & Land Converter",
            shortBenefit = "Convert sq ft, sq meters, acres, hectares & sq miles",
            icon = Icons.Outlined.SquareFoot,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("sqft to sqm", "acres to hectares", "land size")
        ),
        Feature(
            id = "conv_speed",
            toolId = "unit_converter",
            modeId = "Speed",
            title = "Speed Converter",
            shortBenefit = "Convert km/h, mph, m/s, knots & Mach number",
            icon = Icons.Outlined.Speed,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("mph to kmh", "knots to mph", "meters per second")
        ),
        Feature(
            id = "conv_time",
            toolId = "unit_converter",
            modeId = "Time",
            title = "Time Unit Converter",
            shortBenefit = "Convert milliseconds, seconds, minutes, hours, days, weeks & years",
            icon = Icons.Outlined.AccessTime,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("hours to minutes", "days to seconds", "time math")
        ),
        Feature(
            id = "conv_pressure",
            toolId = "unit_converter",
            modeId = "Pressure",
            title = "Pressure Converter",
            shortBenefit = "Convert PSI, bar, kPa, hPa, atm & mmHg/Torr",
            icon = Icons.Outlined.Compress,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("psi to bar", "tire pressure", "kpa", "hpa to inHg")
        ),
        Feature(
            id = "conv_energy",
            toolId = "unit_converter",
            modeId = "Energy",
            title = "Energy & Power Converter",
            shortBenefit = "Convert Joules, kcal, kWh, BTU & electron-volts",
            icon = Icons.Outlined.Bolt,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("kwh to joules", "calories to kilojoules", "btu")
        ),
        Feature(
            id = "conv_data",
            toolId = "unit_converter",
            modeId = "Data",
            title = "Digital Data Converter",
            shortBenefit = "Convert bits, bytes, KB, MB, GB, TB & network transfer rates",
            icon = Icons.Outlined.Storage,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("mb to gb", "megabits to megabytes", "file size units")
        ),
        Feature(
            id = "conv_fuel",
            toolId = "unit_converter",
            modeId = "Fuel",
            title = "Fuel Efficiency Converter",
            shortBenefit = "Convert L/100km, US MPG, UK MPG & km/L",
            icon = Icons.Outlined.LocalGasStation,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("mpg to l100km", "km per liter", "gas mileage")
        ),
        Feature(
            id = "conv_angle",
            toolId = "unit_converter",
            modeId = "Angle",
            title = "Angle Unit Converter",
            shortBenefit = "Convert degrees, radians, gradians, arcminutes & turns",
            icon = Icons.Outlined.Architecture,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("degrees to radians", "gradians", "mils")
        ),
        Feature(
            id = "conv_currency",
            toolId = "unit_converter",
            modeId = "Currency",
            title = "Currency Converter",
            shortBenefit = "Convert 35+ world currencies with offline & custom rates",
            icon = Icons.Outlined.CurrencyExchange,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("exchange rate", "usd to eur", "forex", "money converter")
        ),
        Feature(
            id = "conv_custom_pro",
            toolId = "unit_converter",
            modeId = "Custom",
            title = "Custom Units & Pinned Pairs",
            shortBenefit = "Create custom conversion ratios, pin favorite pairs & export CSV",
            icon = Icons.Outlined.BookmarkAdded,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = true,
            aliases = listOf("custom unit", "favorite conversions", "converter csv"),
            premiumFeature = PremiumFeature.UNIT_CONVERTER_EXTRAS
        ),
        Feature(
            id = "cooking_converter",
            toolId = "cooking_converter",
            modeId = "kitchen",
            title = "Cooking & Recipe Scaler",
            shortBenefit = "Convert cups to grams by ingredient (flour, butter, sugar) & scale servings",
            icon = Icons.Outlined.Restaurant,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("cups to grams", "tablespoons to ml", "oven temp", "baking")
        ),
        Feature(
            id = "number_base_converter",
            toolId = "number_converter",
            modeId = "bases",
            title = "Programmer Base & Roman",
            shortBenefit = "Binary, Octal, Decimal, Hex, Roman numerals & Prime factorizer",
            icon = Icons.Outlined.Tag,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("hex to binary", "roman numerals", "gcd lcm", "prime numbers")
        ),

        // --- Every Calculator ---
        Feature(
            id = "calc_basic",
            toolId = "calculators",
            modeId = "Basic",
            title = "Basic Calculator",
            shortBenefit = "Fast everyday arithmetic with live expression tape & history",
            icon = Icons.Outlined.Calculate,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("math", "add subtract", "plus minus", "calculator")
        ),
        Feature(
            id = "calc_emi_loan",
            toolId = "calculators",
            modeId = "EMI / Loan",
            title = "EMI & Mortgage Calculator",
            shortBenefit = "Monthly payment, total interest & principal breakdown",
            icon = Icons.Outlined.AccountBalance,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("loan calculator", "mortgage", "car payment", "emi")
        ),
        Feature(
            id = "calc_percentage",
            toolId = "calculators",
            modeId = "Percentage",
            title = "Percentage Calculator",
            shortBenefit = "Find X% of Y, % increase/decrease & fraction to percent",
            icon = Icons.Outlined.Percent,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("percent change", "margin", "percentage of")
        ),
        Feature(
            id = "calc_discount",
            toolId = "calculators",
            modeId = "Discount",
            title = "Discount & Sale Calculator",
            shortBenefit = "Calculate final price, stacked discounts & amount saved",
            icon = Icons.Outlined.LocalOffer,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("sale price", "20% off", "shopping discount")
        ),
        Feature(
            id = "calc_gst_vat",
            toolId = "calculators",
            modeId = "GST / VAT",
            title = "GST / VAT Tax Calculator",
            shortBenefit = "Add or remove inclusive/exclusive sales tax at any rate",
            icon = Icons.Outlined.ReceiptLong,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("sales tax", "vat inclusive", "gst exclusive")
        ),
        Feature(
            id = "calc_tip_split",
            toolId = "calculators",
            modeId = "Tip Split",
            title = "Quick Tip Calculator",
            shortBenefit = "Fast restaurant tip % & per-person rounding",
            icon = Icons.Outlined.Payments,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("gratuity", "restaurant tip", "tip per person")
        ),
        Feature(
            id = "calc_date_diff",
            toolId = "calculators",
            modeId = "Date Diff",
            title = "Date Difference & Workdays",
            shortBenefit = "Count exact years, months, days & business days between dates",
            icon = Icons.Outlined.DateRange,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("days between dates", "add days to date", "duration")
        ),
        Feature(
            id = "calc_age",
            toolId = "calculators",
            modeId = "Age",
            title = "Exact Age Calculator",
            shortBenefit = "Calculate chronological age in years, months, days & next birthday",
            icon = Icons.Outlined.Cake,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("birthday calculator", "how old am i", "days lived")
        ),
        Feature(
            id = "calc_scientific",
            toolId = "calculators",
            modeId = "Scientific",
            title = "Scientific Calculator",
            shortBenefit = "Trig (sin/cos/tan), logs, roots, powers, factorials & constants",
            icon = Icons.Outlined.Functions,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = true,
            aliases = listOf("trigonometry", "logarithm", "engineering calculator"),
            premiumFeature = PremiumFeature.SCIENTIFIC_DIY_CALCULATORS
        ),
        Feature(
            id = "calc_diy_paint_tiles",
            toolId = "diy_calculators",
            modeId = "paint",
            title = "DIY Paint, Tiles & Concrete",
            shortBenefit = "Estimate gallons of paint, tile boxes (with waste %) & concrete bags",
            icon = Icons.Outlined.Handyman,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = true,
            aliases = listOf("paint estimator", "tile calculator", "concrete slabs", "flooring"),
            premiumFeature = PremiumFeature.DIY_CALCULATORS
        ),
        Feature(
            id = "calc_finance_compound_sip",
            toolId = "finance_calculators",
            modeId = "compound",
            title = "Compound Interest & SIP",
            shortBenefit = "Project investment growth, SIP returns, loan comparisons & goals",
            icon = Icons.Outlined.TrendingUp,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = true,
            aliases = listOf("investment calculator", "sip planner", "retirement savings"),
            premiumFeature = PremiumFeature.FINANCE_PACK
        ),
        Feature(
            id = "calc_split_bill_items",
            toolId = "split_bill",
            modeId = "items",
            title = "Split the Bill (Itemized)",
            shortBenefit = "Assign receipt items per friend with proportional tax, tip & share card",
            icon = Icons.Outlined.GroupAdd,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = true,
            aliases = listOf("dinner split", "receipt splitter", "who owes what"),
            premiumFeature = PremiumFeature.SPLIT_THE_BILL
        ),
        Feature(
            id = "calc_fuel_trip_cost",
            toolId = "fuel_calculator",
            modeId = "trip",
            title = "Fuel & Road Trip Cost",
            shortBenefit = "Estimate trip gas budget, per-passenger split & tank range",
            icon = Icons.Outlined.DirectionsCar,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("gas cost", "road trip fuel", "mileage cost")
        ),
        Feature(
            id = "calc_unit_price",
            toolId = "unit_price",
            modeId = "compare",
            title = "Unit Price & Bulk Compare",
            shortBenefit = "Compare up to 4 store package sizes to spot the best value",
            icon = Icons.Outlined.ShoppingCartCheckout,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("price per oz", "price per 100g", "grocery compare")
        ),
        Feature(
            id = "calc_weather_feels_like",
            toolId = "weather_calculators",
            modeId = "heat_index",
            title = "Heat Index & Wind Chill",
            shortBenefit = "Compute feels-like temperature, dew point & frostbite wind chill",
            icon = Icons.Outlined.DeviceThermostat,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("humidex", "feels like temp", "wind chill chart", "dew point")
        ),

        // --- Every Note Type, QR Generator, Text & Password Tools ---
        Feature(
            id = "note_text",
            toolId = "open_library_notes",
            modeId = "text",
            title = "Text Note",
            shortBenefit = "Rich offline notes with color tags, search & instant autosave",
            icon = Icons.Outlined.StickyNote2,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("notepad", "memo", "write note", "journal")
        ),
        Feature(
            id = "note_checklist",
            toolId = "open_library_notes",
            modeId = "checklist",
            title = "Checklist & To-Do Note",
            shortBenefit = "Interactive checkable tasks with progress bar & reordering",
            icon = Icons.Outlined.Checklist,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("todo list", "grocery list", "tasks")
        ),
        Feature(
            id = "note_sketch_canvas",
            toolId = "open_library_notes",
            modeId = "sketch",
            title = "Sketch & Handwriting Pad",
            shortBenefit = "Freehand pen, highlighter, grid/lined papers & PNG/PDF export",
            icon = Icons.Outlined.Gesture,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = true,
            aliases = listOf("drawing pad", "whiteboard", "handwritten note"),
            premiumFeature = PremiumFeature.UNLIMITED_NOTES_PENS_PAPERS
        ),
        Feature(
            id = "note_locked_vault",
            toolId = "open_library_notes",
            modeId = "locked",
            title = "Locked Biometric Notes",
            shortBenefit = "Protect private notes and documents with PIN or fingerprint",
            icon = Icons.Outlined.EnhancedEncryption,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = true,
            aliases = listOf("private note", "secret memo", "fingerprint lock"),
            premiumFeature = PremiumFeature.UNLIMITED_NOTES_PENS_PAPERS
        ),
        Feature(
            id = "qr_generator_create",
            toolId = "qr_generator",
            modeId = "create",
            title = "QR Code Generator",
            shortBenefit = "Create QR codes for URLs, Wi-Fi, vCard contacts, Email & SMS",
            icon = Icons.Outlined.QrCode2,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("make qr", "wifi qr code", "vcard qr")
        ),
        Feature(
            id = "qr_generator_styles_pro",
            toolId = "qr_generator",
            modeId = "styles",
            title = "QR Custom Colors & Badges",
            shortBenefit = "Custom foreground/background colors, rounded modules & high-DPI PNG",
            icon = Icons.Outlined.AutoAwesome,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = true,
            aliases = listOf("colored qr", "custom qr design", "branded qr"),
            premiumFeature = PremiumFeature.QR_GENERATOR_STYLES
        ),
        Feature(
            id = "text_word_counter",
            toolId = "text_tools",
            modeId = "count",
            title = "Word & Character Counter",
            shortBenefit = "Count words, characters, sentences, paragraphs & reading time",
            icon = Icons.Outlined.Notes,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("character limit", "essay count", "reading time")
        ),
        Feature(
            id = "text_case_cleanup",
            toolId = "text_tools",
            modeId = "case",
            title = "Case Converter & Line Sort",
            shortBenefit = "UPPER, lower, Title Case, camelCase, remove duplicates & sort A–Z",
            icon = Icons.Outlined.SortByAlpha,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("uppercase", "title case", "deduplicate lines", "snake_case")
        ),
        Feature(
            id = "password_generator",
            toolId = "password_generator",
            modeId = "password",
            title = "Password & Passphrase Gen",
            shortBenefit = "Cryptographic random passwords, 4-word diceware passphrases & PINs",
            icon = Icons.Outlined.Password,
            category = ToolCategory.CONVERT_AND_WRITE,
            isPremium = false,
            aliases = listOf("strong password", "entropy", "passphrase", "pin generator")
        )
    )

    private data class PrecomputedSearchEntry(
        val feature: Feature,
        val lowerTitle: String,
        val lowerBenefit: String,
        val lowerCategory: String,
        val lowerToolId: String,
        val lowerAliases: List<String>
    )

    private val featureById: Map<String, Feature> = allFeatures.associateBy { it.id }

    private val precomputedSearchEntries: List<PrecomputedSearchEntry> = allFeatures.map { f ->
        PrecomputedSearchEntry(
            feature = f,
            lowerTitle = f.title.lowercase(),
            lowerBenefit = f.shortBenefit.lowercase(),
            lowerCategory = f.category.title.lowercase(),
            lowerToolId = f.toolId.lowercase(),
            lowerAliases = f.aliases.map { it.lowercase() }
        )
    }

    /** Total number of distinct registered features (never inflated). */
    val totalCount: Int = allFeatures.size

    /** Total number of Premium features in the catalog. */
    val premiumCount: Int = allFeatures.count { it.isPremium }

    /** Total number of Free features in the catalog. */
    val freeCount: Int = allFeatures.count { !it.isPremium }

    /** Live formatted counter string ("N features, M Premium"). */
    val liveCounterText: String = "$totalCount features, $premiumCount Premium"

    /** All Premium features in catalog order. */
    val premiumFeatures: List<Feature> = allFeatures.filter { it.isPremium }

    /** All Free features in catalog order. */
    val freeFeatures: List<Feature> = allFeatures.filter { !it.isPremium }

    fun getFeatureById(id: String): Feature? = featureById[id]

    /**
     * Resolves either a Feature ID or a legacy Tool ID to a Feature.
     */
    fun resolveFeatureOrPrimaryForTool(idOrToolId: String): Feature? {
        val clean = idOrToolId.substringBefore(":")
        val modePart = idOrToolId.substringAfter(":", "").ifEmpty { null }
        if (modePart != null) {
            resolveFeatureForToolAndMode(clean, modePart)?.let { return it }
        }
        featureById[clean]?.let { return it }
        return allFeatures.firstOrNull { it.toolId == clean }
    }

    /**
     * Resolves a Feature given a toolId (or featureId) and an optional modeId.
     */
    fun resolveFeatureForToolAndMode(toolId: String, modeId: String?): Feature? {
        featureById[toolId]?.let { return it }
        if (!modeId.isNullOrBlank()) {
            featureById[modeId]?.let { return it }
            allFeatures.firstOrNull {
                it.toolId.equals(toolId, ignoreCase = true) &&
                    it.modeId != null &&
                    it.modeId.equals(modeId, ignoreCase = true)
            }?.let { return it }
            allFeatures.firstOrNull {
                (it.toolId == "bubble_level" && toolId == "level" && it.modeId.equals(modeId, ignoreCase = true)) ||
                    (it.toolId == "flashlight" && toolId in setOf("strobe_torch", "screen_torch") && it.modeId.equals(modeId, ignoreCase = true))
            }?.let { return it }
        }
        return allFeatures.firstOrNull { it.toolId.equals(toolId, ignoreCase = true) }
    }

    /**
     * Returns the effective inner mode identifier for a catalog [Feature].
     */
    fun resolveModeIdForFeature(feature: Feature): String {
        return feature.modeId ?: feature.id
    }

    /**
     * Returns all features for a given ToolCategory.
     */
    fun getFeaturesByCategory(
        category: ToolCategory,
        capabilities: DeviceCapabilities? = null,
        includeUnsupported: Boolean = false
    ): List<Feature> {
        return allFeatures.filter { feature ->
            feature.category == category &&
                (includeUnsupported || capabilities == null || isSupported(feature, capabilities))
        }
    }

    /**
     * Returns whether this phone supports all hardware required by [feature].
     */
    fun isSupported(feature: Feature, capabilities: DeviceCapabilities): Boolean {
        return capabilities.isToolSupported(feature.requiredCapabilities)
    }

    fun isSupported(feature: Feature, snapshot: com.example.data.DeviceCapabilitiesSnapshot): Boolean {
        return snapshot.supportsAll(feature.requiredCapabilities)
    }

    /**
     * Returns human-readable reason if a feature is not supported on this device, or null if supported.
     */
    fun unsupportedReason(feature: Feature, capabilities: DeviceCapabilities): String? {
        return capabilities.getMissingReason(feature.requiredCapabilities)
    }

    fun unsupportedReason(feature: Feature, snapshot: com.example.data.DeviceCapabilitiesSnapshot): String? {
        return snapshot.firstMissingReason(feature.requiredCapabilities)
    }

    /**
     * Searches features by title, shortBenefit, category title, or aliases using precomputed lowercase index.
     */
    fun searchFeatures(
        query: String,
        capabilities: DeviceCapabilities? = null,
        includeUnsupported: Boolean = true
    ): List<Feature> {
        val trimmed = query.trim().lowercase()
        val entries = if (includeUnsupported || capabilities == null) {
            precomputedSearchEntries
        } else {
            precomputedSearchEntries.filter { isSupported(it.feature, capabilities) }
        }
        if (trimmed.isEmpty()) return entries.map { it.feature }
        return entries.filter { entry ->
            entry.lowerTitle.contains(trimmed) ||
                entry.lowerBenefit.contains(trimmed) ||
                entry.lowerCategory.contains(trimmed) ||
                entry.lowerAliases.any { it.contains(trimmed) } ||
                entry.lowerToolId.contains(trimmed)
        }.sortedWith(
            compareByDescending<PrecomputedSearchEntry> { it.lowerTitle.startsWith(trimmed) }
                .thenByDescending { entry -> entry.lowerAliases.any { it.startsWith(trimmed) } }
                .thenBy { it.feature.title }
        ).map { it.feature }
    }

    /**
     * Default built-in sections for Home.
     */
    fun getDefaultSections(capabilities: DeviceCapabilities? = null): List<CustomHomeSection> {
        return ToolCategory.entries
            .sortedBy { it.ordinal }
            .map { category ->
                val color = when (category) {
                    ToolCategory.MEDIA_STUDIO -> 0xFFF472B6
                    ToolCategory.PHONE_CARE_AND_AUDIO -> 0xFF38BDF8
                    ToolCategory.MEASURE -> 0xFF34D399
                    ToolCategory.LIGHT_AND_SOUND -> 0xFFFBBF24
                    ToolCategory.TRAVEL_AND_TIME -> 0xFFA78BFA
                    ToolCategory.CONVERT_AND_WRITE -> 0xFFFB923C
                }
                val iconName = when (category) {
                    ToolCategory.MEDIA_STUDIO -> "AutoFixHigh"
                    ToolCategory.PHONE_CARE_AND_AUDIO -> "Speaker"
                    ToolCategory.MEASURE -> "Straighten"
                    ToolCategory.LIGHT_AND_SOUND -> "FlashlightOn"
                    ToolCategory.TRAVEL_AND_TIME -> "Explore"
                    ToolCategory.CONVERT_AND_WRITE -> "Calculate"
                }
                CustomHomeSection(
                    id = "builtin_${category.name.lowercase()}",
                    name = category.title,
                    colorHex = color,
                    iconName = iconName,
                    isBuiltIn = true,
                    builtInCategory = category,
                    isHidden = false,
                    layoutStyle = SectionLayoutStyle.ROW,
                    featureIds = getFeaturesByCategory(category, capabilities, includeUnsupported = false).map { it.id }
                )
            }
    }

    /**
     * Serializes a list of [CustomHomeSection] to JSON for DataStore and ZIP backup.
     */
    fun serializeSectionsToJson(sections: List<CustomHomeSection>): String {
        val array = JSONArray()
        sections.forEach { sec ->
            val obj = JSONObject()
            obj.put("id", sec.id)
            obj.put("name", sec.name)
            obj.put("colorHex", sec.colorHex)
            obj.put("iconName", sec.iconName)
            obj.put("isBuiltIn", sec.isBuiltIn)
            obj.put("builtInCategory", sec.builtInCategory?.name ?: "")
            obj.put("isHidden", sec.isHidden)
            obj.put("layoutStyle", sec.layoutStyle.code)
            val fArr = JSONArray()
            sec.featureIds.forEach { fArr.put(it) }
            obj.put("featureIds", fArr)
            array.put(obj)
        }
        return array.toString()
    }

    /**
     * Deserializes custom & built-in sections from JSON, merging any newly added catalog features.
     */
    fun parseSectionsFromJson(
        json: String,
        capabilities: DeviceCapabilities? = null
    ): List<CustomHomeSection> {
        val defaults = getDefaultSections(capabilities)
        if (json.isBlank()) return defaults
        return try {
            val array = JSONArray(json)
            if (array.length() == 0) return defaults
            val parsed = mutableListOf<CustomHomeSection>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val catName = obj.optString("builtInCategory", "")
                val builtInCat = ToolCategory.entries.firstOrNull { it.name == catName }
                val fArr = obj.optJSONArray("featureIds")
                val fIds = mutableListOf<String>()
                if (fArr != null) {
                    for (j in 0 until fArr.length()) {
                        val fid = fArr.optString(j)
                        if (fid.isNotBlank() && featureById.containsKey(fid)) {
                            if (capabilities == null || isSupported(featureById.getValue(fid), capabilities)) {
                                fIds.add(fid)
                            }
                        }
                    }
                }
                // Ensure any new features from built-in category not placed elsewhere are retained
                if (builtInCat != null && fIds.isEmpty()) {
                    fIds.addAll(
                        getFeaturesByCategory(builtInCat, capabilities, includeUnsupported = false).map { it.id }
                    )
                }
                parsed.add(
                    CustomHomeSection(
                        id = obj.optString("id", "sec_$i"),
                        name = obj.optString("name", builtInCat?.title ?: "Section"),
                        colorHex = obj.optLong("colorHex", 0xFF38BDF8L),
                        iconName = obj.optString("iconName", "Folder"),
                        isBuiltIn = obj.optBoolean("isBuiltIn", builtInCat != null),
                        builtInCategory = builtInCat,
                        isHidden = obj.optBoolean("isHidden", false),
                        layoutStyle = SectionLayoutStyle.fromCode(obj.optString("layoutStyle", "row")),
                        featureIds = fIds.distinct()
                    )
                )
            }
            parsed.ifEmpty { defaults }
        } catch (_: Exception) {
            defaults
        }
    }

    /**
     * Parses per-tile size map (`featureId:S|M|L`).
     */
    fun parseTileSizes(csv: String): Map<String, FeatureTileSize> {
        if (csv.isBlank()) return emptyMap()
        return csv.split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    parts[0].trim() to FeatureTileSize.fromCode(parts[1].trim())
                } else null
            }
            .toMap()
    }

    fun serializeTileSizes(map: Map<String, FeatureTileSize>): String {
        return map.entries
            .filter { it.value != FeatureTileSize.MEDIUM }
            .joinToString(",") { "${it.key}:${it.value.code}" }
    }

    /**
     * Parses saved Home layouts JSON (up to 5 named layouts).
     */
    fun parseSavedLayouts(json: String): List<SavedHomeLayout> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<SavedHomeLayout>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    SavedHomeLayout(
                        id = o.optString("id", "layout_$i"),
                        name = o.optString("name", "Layout ${i + 1}"),
                        createdAtMs = o.optLong("createdAtMs", 0L),
                        sectionsJson = o.optString("sectionsJson", ""),
                        hiddenFeatureIdsCsv = o.optString("hiddenFeatureIdsCsv", ""),
                        perTileSizeCsv = o.optString("perTileSizeCsv", ""),
                        pinnedTopFeatureIdsCsv = o.optString("pinnedTopFeatureIdsCsv", ""),
                        startTabIndex = o.optInt("startTabIndex", 0),
                        homeFirstFeatureId = o.optString("homeFirstFeatureId", "")
                    )
                )
            }
            result.take(MAX_SAVED_HOME_LAYOUTS)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeSavedLayouts(layouts: List<SavedHomeLayout>): String {
        val arr = JSONArray()
        layouts.take(MAX_SAVED_HOME_LAYOUTS).forEach { l ->
            val o = JSONObject()
            o.put("id", l.id)
            o.put("name", l.name)
            o.put("createdAtMs", l.createdAtMs)
            o.put("sectionsJson", l.sectionsJson)
            o.put("hiddenFeatureIdsCsv", l.hiddenFeatureIdsCsv)
            o.put("perTileSizeCsv", l.perTileSizeCsv)
            o.put("pinnedTopFeatureIdsCsv", l.pinnedTopFeatureIdsCsv)
            o.put("startTabIndex", l.startTabIndex)
            o.put("homeFirstFeatureId", l.homeFirstFeatureId)
            arr.put(o)
        }
        return arr.toString()
    }

    /**
     * Android ShortcutManager integration:
     * - Pin any feature as a home screen launcher shortcut
     * - Sync up to 4 dynamic app shortcuts for Favorites
     */
    fun requestPinFeatureShortcut(context: Context, feature: Feature): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!shortcutManager.isRequestPinShortcutSupported) return false

        val shortcutInfo = buildShortcutInfo(context, feature)
        val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(shortcutInfo)
        val successCallback = PendingIntent.getBroadcast(
            context,
            feature.id.hashCode(),
            pinnedShortcutCallbackIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return shortcutManager.requestPinShortcut(shortcutInfo, successCallback.intentSender)
    }

    fun syncFavoriteDynamicShortcuts(context: Context, favoriteIds: List<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
            val features = favoriteIds
                .mapNotNull { resolveFeatureOrPrimaryForTool(it) }
                .distinctBy { it.id }
                .take(4)
            val shortcuts = features.map { buildShortcutInfo(context, it) }
            shortcutManager.dynamicShortcuts = shortcuts
        } catch (_: Exception) {
            // Ignore on launchers that restrict dynamic shortcuts
        }
    }

    private fun buildShortcutInfo(context: Context, feature: Feature): ShortcutInfo {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_SHORTCUT_FEATURE_ID, feature.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return ShortcutInfo.Builder(context, "dyne_feature_${feature.id}")
            .setShortLabel(feature.title.take(22))
            .setLongLabel("${feature.title} — ${feature.shortBenefit}".take(48))
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(launchIntent)
            .build()
    }
}
