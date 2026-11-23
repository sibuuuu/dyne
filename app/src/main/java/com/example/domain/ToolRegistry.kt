package com.example.domain

import android.Manifest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterTiltShift
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.HardwareCapability
import com.example.ui.components.CustomToolIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ToolCategory(val title: String, val accentColor: Color = Color(0xFFB6FF2B)) {
    MEDIA_STUDIO("Media Studio", Color(0xFFFF2D55)),
    PHONE_CARE_AND_AUDIO("Phone Care & Audio", Color(0xFF00E5FF)),
    MEASURE("Measure", Color(0xFF28C840)),
    LIGHT_AND_SOUND("Light & Sound", Color(0xFFFFB800)),
    TRAVEL_AND_TIME("Travel & Time", Color(0xFF32ADE6)),
    CONVERT_AND_WRITE("Convert & Write", Color(0xFFBF5AF2))
}

enum class HomeNeedPreset(
    val label: String,
    val toolIds: Set<String>
) {
    OUTDOOR(
        label = "Outdoor",
        toolIds = setOf(
            "speaker_cleaner", "frequency_tool", "compass", "compass_waypoints", "waypoints_pro",
            "qibla_finder", "parking_saver", "weather_calculators", "pedometer", "pedometer_history",
            "speedometer", "altimeter", "flashlight", "level", "walk_run_tracker", "fishing_log",
            "world_clock", "quick_note", "trip_gear_checklists", "fuel_trip_calc", "magnifier_color"
        )
    ),
    DIY(
        label = "DIY",
        toolIds = setOf(
            "level", "level_calibration_pro", "measurement_history_csv", "angle", "ruler",
            "diy_calculators", "metal_finder", "sound_db", "lux_meter", "photo_light_meter",
            "vibration_gforce", "flashlight", "unit_converter", "tip_calc", "speaker_cleaner",
            "magnifier_color"
        )
    ),
    TRAVEL(
        label = "Travel",
        toolIds = setOf(
            "world_clock", "unit_converter", "compass", "compass_waypoints", "qibla_finder",
            "parking_saver", "weather_calculators", "pedometer", "speedometer", "qr_generator",
            "quick_note", "flashlight", "tip_calc", "noise_sleep_mixer", "pdf_tools",
            "fuel_trip_calc", "split_bill_pro", "trip_gear_checklists", "event_countdown"
        )
    ),
    STUDY(
        label = "Study",
        toolIds = setOf(
            "quick_note", "pdf_tools", "unit_converter", "tip_calc", "scientific_calc",
            "stopwatch", "pomodoro_timer", "pomodoro_stats", "interval_timer",
            "noise_sleep_mixer", "frequency_tool", "qr_generator", "text_tools", "number_tools"
        )
    ),
    OFFICE(
        label = "Office",
        toolIds = setOf(
            "pdf_tools", "quick_note", "qr_generator", "world_clock", "unit_converter",
            "tip_calc", "finance_pack", "password_generator", "text_tools", "image_toolkit",
            "photo_converter", "background_remover", "photo_editor", "passport_id_photo",
            "audio_converter", "video_tools", "pdf_image_tools",
            "stopwatch", "device_sensors", "speaker_cleaner"
        )
    )
}

data class ToolMode(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val isPremium: Boolean = false
)

data class RegisteredTool(
    val id: String,
    val name: String,
    val description: String,
    val shortBenefit: String = description,
    val category: ToolCategory,
    val icon: ImageVector,
    val isPro: Boolean = false,
    val isEnabled: Boolean = true,
    val requiredPermission: String? = null,
    val requiredCapabilities: Set<HardwareCapability> = emptySet(),
    val heroGradientHex: Pair<Long, Long> = 0xFF14213DL to 0xFF1F3A60L,
    val keywords: List<String> = emptyList(),
    val modes: List<ToolMode> = emptyList()
) {
    val subtitle: String
        get() = shortBenefit.ifBlank { description }

    val isProOnly: Boolean
        get() = isPro
}

data class ToolOrModeSearchResult(
    val tool: RegisteredTool,
    val mode: ToolMode? = null,
    val displayTitle: String = if (mode != null) "${tool.name} > ${mode.title}" else tool.name,
    val subtitle: String = if (mode != null) "Mode in ${tool.name} • ${tool.category.title}" else tool.shortBenefit
)

object ToolRegistry {

    val MORE_MODE = ToolMode(
        id = "more",
        title = "More",
        icon = Icons.Default.GridView,
        isPremium = false
    )

    private fun withMore(vararg modes: ToolMode): List<ToolMode> =
        modes.toList() + MORE_MODE

    private val defaultToolsList: List<RegisteredTool> = listOf(
        // ====================================================================
        // 1. PHONE CARE & AUDIO
        // ====================================================================
        RegisteredTool(
            id = "speaker_cleaner",
            name = "Speaker Cleaner",
            description = "165 Hz water eject, dust pulse & post-clean stereo test.",
            shortBenefit = "Eject trapped water & dust in 30s",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = CustomToolIcons.SpeakerCleaner,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.AUDIO_OUTPUT),
            heroGradientHex = 0xFF0A2E46L to 0xFF007791L,
            keywords = listOf("water", "eject", "dust", "speaker", "clean", "muffled"),
            modes = withMore(
                ToolMode("water", "Water", Icons.Default.WaterDrop, isPremium = false),
                ToolMode("dust", "Dust", Icons.Default.Air, isPremium = false),
                ToolMode("test", "Test", Icons.AutoMirrored.Filled.VolumeUp, isPremium = false),
                ToolMode("timer", "Timer", Icons.Default.Timer, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "frequency_tool",
            name = "Frequency",
            description = "20 Hz–20 kHz oscillator, sweep, binaural & sound packs.",
            shortBenefit = "Precision 48 kHz sine, sweep & sound packs",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = CustomToolIcons.FrequencyWave,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.AUDIO_OUTPUT),
            heroGradientHex = 0xFF2D1B4EL to 0xFF56328CL,
            keywords = listOf("hz", "sine", "square", "triangle", "sawtooth", "sweep", "tone", "pulse", "presets", "packs"),
            modes = withMore(
                ToolMode("tone", "Tone", CustomToolIcons.FrequencyWave, isPremium = false),
                ToolMode("sweep", "Sweep", Icons.Default.Timeline, isPremium = false),
                ToolMode("pulse", "Pulse", Icons.Default.GraphicEq, isPremium = false),
                ToolMode("presets", "Presets", Icons.Default.Star, isPremium = false),
                ToolMode("packs", "Packs", Icons.Default.LibraryMusic, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "speaker_test",
            name = "Speaker Test",
            description = "Left/Right stereo check, bass 60 Hz, mid 1 kHz, treble 8 kHz & polarity.",
            shortBenefit = "Verify Left/Right stereo & frequency response",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.Headphones,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.AUDIO_OUTPUT),
            heroGradientHex = 0xFF1A2A4AL to 0xFF274472L,
            keywords = listOf("stereo", "left", "right", "bass", "treble", "speaker"),
            modes = withMore(
                ToolMode("channels", "L / R Check", Icons.Default.Headphones),
                ToolMode("bands", "Bass / Treble", Icons.Default.Equalizer),
                ToolMode("polarity", "Phase Check", Icons.Default.CompareArrows)
            )
        ),
        RegisteredTool(
            id = "mic_test",
            name = "Mic Test",
            description = "Live microphone waveform, peak dBFS meter & 3-second local loopback test.",
            shortBenefit = "Check microphone clarity & live input level",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.Mic,
            isPro = false,
            requiredPermission = Manifest.permission.RECORD_AUDIO,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            heroGradientHex = 0xFF133337L to 0xFF1E565CL,
            keywords = listOf("microphone", "voice", "record", "input", "loopback"),
            modes = withMore(
                ToolMode("waveform", "Waveform", Icons.Default.Mic),
                ToolMode("loopback", "3s Loopback", Icons.AutoMirrored.Filled.VolumeUp),
                ToolMode("peaks", "Peak Meter", Icons.Default.BarChart)
            )
        ),
        RegisteredTool(
            id = "tone_generator",
            name = "Musical Tones",
            description = "Concert A4=440 Hz reference notes C4–B5 with fine cents tuning.",
            shortBenefit = "Play exact musical reference pitches",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.MusicNote,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.AUDIO_OUTPUT),
            heroGradientHex = 0xFF282048L to 0xFF433575L,
            keywords = listOf("note", "pitch", "440", "piano", "concert"),
            modes = withMore(
                ToolMode("notes", "Note Grid", Icons.Default.MusicNote),
                ToolMode("octave", "Octave Shift", Icons.Default.Tune)
            )
        ),
        RegisteredTool(
            id = "noise_sleep_mixer",
            name = "Noise & Sleep",
            description = "White, pink & brown noise synthesizer with sleep fade timer.",
            shortBenefit = "Synthesized white, pink & brown noise",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.Nightlight,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.AUDIO_OUTPUT),
            heroGradientHex = 0xFF102038L to 0xFF1D3B66L,
            keywords = listOf("white noise", "pink noise", "brown noise", "sleep", "relax"),
            modes = withMore(
                ToolMode("mixer", "Noise Mixer", Icons.Default.Waves),
                ToolMode("sleep_timer", "Sleep Timer", Icons.Default.Timer)
            )
        ),
        RegisteredTool(
            id = "vibration_lab",
            name = "Vibration Test",
            description = "Test haptic motor patterns: Continuous, Pulse, Heartbeat, SOS & Ramp.",
            shortBenefit = "Verify haptic motor strength & patterns",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.Vibration,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.VIBRATOR),
            heroGradientHex = 0xFF2A1E3FL to 0xFF4B3670L,
            keywords = listOf("vibrate", "haptic", "motor", "pulse"),
            modes = withMore(
                ToolMode("patterns", "Patterns", Icons.Default.Vibration),
                ToolMode("intensity", "Intensity", Icons.Default.Tune)
            )
        ),
        RegisteredTool(
            id = "touch_screen_test",
            name = "Touch Test",
            description = "Full-screen multi-touch grid & dead-zone visualizer.",
            shortBenefit = "Inspect multi-touch & screen dead zones",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.TouchApp,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.MULTITOUCH),
            heroGradientHex = 0xFF183048L to 0xFF285078L,
            keywords = listOf("touch", "screen", "display", "digitizer", "grid"),
            modes = withMore(
                ToolMode("grid", "Touch Grid", Icons.Default.TouchApp),
                ToolMode("multitouch", "Multi-Touch", Icons.Default.CenterFocusStrong)
            )
        ),
        RegisteredTool(
            id = "metronome",
            name = "Metronome",
            description = "30–240 BPM precision click track with tap tempo & time signatures.",
            shortBenefit = "30–240 BPM click track & tap tempo",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.AvTimer,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.AUDIO_OUTPUT),
            heroGradientHex = 0xFF1F2E2EL to 0xFF335252L,
            keywords = listOf("bpm", "tempo", "rhythm", "beat", "click"),
            modes = withMore(
                ToolMode("pendulum", "BPM Click", Icons.Default.AvTimer),
                ToolMode("tap", "Tap Tempo", Icons.Default.TouchApp)
            )
        ),
        RegisteredTool(
            id = "tuner",
            name = "Chromatic Tuner",
            description = "Autocorrelation pitch detector with ±50 cents needle & Guitar/Ukulele presets.",
            shortBenefit = "Tune guitar, bass, violin & ukulele",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.Tune,
            isPro = false,
            requiredPermission = Manifest.permission.RECORD_AUDIO,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            heroGradientHex = 0xFF19323CL to 0xFF2B5868L,
            keywords = listOf("guitar", "ukulele", "violin", "pitch", "hz", "cents"),
            modes = withMore(
                ToolMode("chromatic", "Chromatic", Icons.Default.Tune),
                ToolMode("guitar", "Guitar / Uke", Icons.Default.MusicNote)
            )
        ),
        RegisteredTool(
            id = "battery_info",
            name = "Battery Health",
            description = "Live voltage (mV), temperature (°C/°F), health status & current (mA).",
            shortBenefit = "Inspect battery voltage, temp & health",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.BatteryChargingFull,
            isPro = false,
            heroGradientHex = 0xFF143628L to 0xFF215E46L,
            keywords = listOf("battery", "voltage", "charging", "temperature", "mah"),
            modes = withMore(
                ToolMode("telemetry", "Live Status", Icons.Default.BatteryChargingFull),
                ToolMode("thermal", "Thermal & mV", Icons.Default.Thermostat)
            )
        ),
        RegisteredTool(
            id = "screen_test",
            name = "Screen Test",
            description = "Full-screen dead pixel inspector, RGB/CMYK color purity & gradient banding check.",
            shortBenefit = "Dead pixel & color purity check",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.Smartphone,
            isPro = false,
            heroGradientHex = 0xFF1A2845L to 0xFF2C4578L,
            keywords = listOf("screen", "dead pixel", "color", "display", "lcd", "oled", "banding", "test"),
            modes = withMore(
                ToolMode("dead_pixel", "Dead Pixel", Icons.Default.Smartphone),
                ToolMode("gradients", "Color & Banding", Icons.Default.Palette),
                ToolMode("grid", "Alignment Grid", Icons.Default.GridView)
            )
        ),

        // ====================================================================
        // 2. MEASURE
        // ====================================================================
        RegisteredTool(
            id = "level",
            name = "Bubble Level",
            description = "Approximate 2D bullseye spirit level & vertical plumb vial with calibration profiles & CSV history.",
            shortBenefit = "2D bullseye & vertical spirit level (approx.)",
            category = ToolCategory.MEASURE,
            icon = CustomToolIcons.BubbleLevel,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            heroGradientHex = 0xFF143326L to 0xFF205740L,
            keywords = listOf("level", "spirit", "bubble", "surface", "plumb", "calibrate", "profiles", "angle", "flat", "csv"),
            modes = withMore(
                ToolMode("bubble", "Bubble", CustomToolIcons.BubbleLevel, isPremium = false),
                ToolMode("surface", "Surface", Icons.Default.FilterTiltShift, isPremium = false),
                ToolMode("plumb", "Plumb", Icons.Default.Height, isPremium = false),
                ToolMode("calibrate", "Calibrate", Icons.Default.CompassCalibration, isPremium = false),
                ToolMode("calibration_profiles", "Profiles", Icons.Default.Tune, isPremium = true),
                ToolMode("angle", "Angle", CustomToolIcons.AngleMeter, isPremium = true),
                ToolMode("history", "History & CSV", Icons.Default.History, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "level_calibration_pro",
            name = "Level Profiles",
            description = "Save, switch & reset multiple named surface pitch & roll calibration profiles (approximate).",
            shortBenefit = "Saved multi-surface level calibration profiles",
            category = ToolCategory.MEASURE,
            icon = Icons.Default.CompassCalibration,
            isPro = true,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            heroGradientHex = 0xFF193829L to 0xFF2B6147L,
            keywords = listOf("level", "calibration", "profiles", "zero", "offset", "surface", "pitch", "roll"),
            modes = withMore(
                ToolMode("calibration_profiles", "Profiles", Icons.Default.Tune, isPremium = true),
                ToolMode("history", "History & CSV", Icons.Default.History, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "measurement_history_csv",
            name = "Measure History",
            description = "Record approximate readings for Level, Angle, Compass & Sound Meter and export to RFC-4180 CSV.",
            shortBenefit = "Level, Angle, Compass & Sound CSV log",
            category = ToolCategory.MEASURE,
            icon = Icons.Default.History,
            isPro = true,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            heroGradientHex = 0xFF1E2D3DL to 0xFF334E68L,
            keywords = listOf("measurement history", "csv", "export", "level", "angle", "compass", "sound meter", "log"),
            modes = withMore(
                ToolMode("level_log", "Level Log", CustomToolIcons.BubbleLevel, isPremium = true),
                ToolMode("angle_log", "Angle Log", CustomToolIcons.AngleMeter, isPremium = true),
                ToolMode("compass_log", "Compass Log", Icons.Default.Explore, isPremium = true),
                ToolMode("sound_log", "Sound dB Log", Icons.Default.GraphicEq, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "angle",
            name = "Angle",
            description = "Approximate inclinometer protractor with Hold, Zero calibration & CSV measurement history.",
            shortBenefit = "Slope & degree protractor (approx.)",
            category = ToolCategory.MEASURE,
            icon = CustomToolIcons.AngleMeter,
            isPro = true,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            heroGradientHex = 0xFF332614L to 0xFF5C4322L,
            keywords = listOf("angle", "inclinometer", "slope", "protractor", "degree", "pitch", "csv"),
            modes = withMore(
                ToolMode("angle", "Angle", CustomToolIcons.AngleMeter, isPremium = true),
                ToolMode("bubble", "Bubble", CustomToolIcons.BubbleLevel, isPremium = false),
                ToolMode("surface", "Surface", Icons.Default.FilterTiltShift, isPremium = false),
                ToolMode("plumb", "Plumb", Icons.Default.Height, isPremium = false),
                ToolMode("calibrate", "Calibrate", Icons.Default.CompassCalibration, isPremium = false),
                ToolMode("history", "History & CSV", Icons.Default.History, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "ruler",
            name = "Screen Ruler",
            description = "Display-DPI calibrated edge ruler in cm/mm and inches with dual calipers.",
            shortBenefit = "True-DPI cm & inch screen ruler",
            category = ToolCategory.MEASURE,
            icon = Icons.Default.Straighten,
            isPro = false,
            heroGradientHex = 0xFF1B2B44L to 0xFF2E4A75L,
            keywords = listOf("ruler", "screen ruler", "measure", "cm", "mm", "inch", "dpi", "length"),
            modes = withMore(
                ToolMode("edge", "Inch & cm Scale", Icons.Default.Straighten),
                ToolMode("caliper", "Dual Caliper", Icons.Default.SwapHoriz),
                ToolMode("calibrate", "DPI Calibrate", Icons.Default.Tune)
            )
        ),
        RegisteredTool(
            id = "diy_calculators",
            name = "DIY Calculators",
            description = "Room paint liters & cans, tile count with wastage, concrete slab m³/bags & flooring packs.",
            shortBenefit = "Paint, tiles, concrete & flooring estimators",
            category = ToolCategory.MEASURE,
            icon = Icons.Default.SquareFoot,
            isPro = true,
            heroGradientHex = 0xFF2F2416L to 0xFF574228L,
            keywords = listOf("diy", "paint", "tiles", "concrete", "flooring", "renovation", "area", "slab"),
            modes = withMore(
                ToolMode("paint", "Paint", Icons.Default.Brush, isPremium = true),
                ToolMode("tiles", "Tiles", Icons.Default.GridView, isPremium = true),
                ToolMode("concrete", "Concrete", Icons.Default.Storage, isPremium = true),
                ToolMode("flooring", "Flooring", Icons.Default.SquareFoot, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "metal_finder",
            name = "Metal Detector",
            description = "Approximate magnetic field µT gauge with tare baseline & proximity beep.",
            shortBenefit = "Detect ferromagnetic metal & studs in µT",
            category = ToolCategory.MEASURE,
            icon = CustomToolIcons.MetalDetector,
            isPro = true,
            requiredCapabilities = setOf(HardwareCapability.MAGNETOMETER),
            heroGradientHex = 0xFF2E2218L to 0xFF543D2BL,
            keywords = listOf("metal", "magnet", "stud", "emf", "ut", "detector"),
            modes = withMore(
                ToolMode("gauge", "Field Gauge", CustomToolIcons.MetalDetector, isPremium = true),
                ToolMode("graph", "Flux Graph", Icons.Default.ShowChart, isPremium = true),
                ToolMode("peak", "Peak Finder", Icons.Default.Radar, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "lux_meter",
            name = "Lux Meter",
            description = "Approximate ambient light sensor in lx and foot-candles with scene labels & Photo EV mode.",
            shortBenefit = "Ambient light sensor with scene labels (approx.)",
            category = ToolCategory.MEASURE,
            icon = Icons.Default.LightMode,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.LIGHT),
            heroGradientHex = 0xFF332B12L to 0xFF594B1FL,
            keywords = listOf("lux", "light", "footcandle", "brightness", "lumen", "scene", "photography", "ev"),
            modes = withMore(
                ToolMode("meter", "Lux & Scenes", Icons.Default.LightMode, isPremium = false),
                ToolMode("reference", "Scene Guide", Icons.Default.WbSunny, isPremium = false),
                ToolMode("photo_ev", "Photo Light Meter", Icons.Default.CameraAlt, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "photo_light_meter",
            name = "Photo Light Meter",
            description = "Approximate Exposure Value (EV), aperture (f-stop), shutter speed & ISO calculator from ambient lux.",
            shortBenefit = "EV, aperture, shutter & ISO light meter",
            category = ToolCategory.MEASURE,
            icon = Icons.Default.CameraAlt,
            isPro = true,
            requiredCapabilities = setOf(HardwareCapability.LIGHT),
            heroGradientHex = 0xFF362A14L to 0xFF614B24L,
            keywords = listOf("photography", "light meter", "ev", "exposure", "aperture", "shutter", "iso", "lux"),
            modes = withMore(
                ToolMode("photo_ev", "EV & Exposure", Icons.Default.CameraAlt, isPremium = true),
                ToolMode("meter", "Lux & Scenes", Icons.Default.LightMode, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "vibration_gforce",
            name = "Vibration & G-Force",
            description = "Approximate 3-axis accelerometer vibration meter (m/s²) and G-force meter with peak hold.",
            shortBenefit = "Vibration & G-force meter with peak hold",
            category = ToolCategory.MEASURE,
            icon = Icons.Default.Timeline,
            isPro = true,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER),
            heroGradientHex = 0xFF321B2EL to 0xFF592E52L,
            keywords = listOf("vibration", "gforce", "g-force", "peak hold", "accelerometer", "shock", "seismic"),
            modes = withMore(
                ToolMode("vibration", "Vibration Meter", Icons.Default.Vibration, isPremium = true),
                ToolMode("gforce", "G-Force & Peak", Icons.Default.Timeline, isPremium = true),
                ToolMode("waveform", "Waveform", Icons.Default.ShowChart, isPremium = true)
            )
        ),

        // ====================================================================
        // 3. LIGHT & SOUND
        // ====================================================================
        RegisteredTool(
            id = "flashlight",
            name = "Flashlight",
            description = "Hardware LED torch, brightness levels, SOS, screen light, timer, strobe, Morse & colors.",
            shortBenefit = "LED torch, SOS, strobe, Morse & screen light",
            category = ToolCategory.LIGHT_AND_SOUND,
            icon = Icons.Default.FlashlightOn,
            isPro = false,
            requiredPermission = Manifest.permission.CAMERA,
            requiredCapabilities = setOf(HardwareCapability.FLASH),
            heroGradientHex = 0xFF2E2A12L to 0xFF544D1EL,
            keywords = listOf("flashlight", "torch", "sos", "strobe", "morse", "screen light", "colors", "brightness"),
            modes = withMore(
                ToolMode("normal", "Normal", Icons.Default.FlashlightOn, isPremium = false),
                ToolMode("brightness", "Brightness", Icons.Default.Lightbulb, isPremium = false),
                ToolMode("sos", "SOS", Icons.Default.Bolt, isPremium = false),
                ToolMode("screen_light", "Screen Light", Icons.Default.Smartphone, isPremium = false),
                ToolMode("timer", "Timer", Icons.Default.Timer, isPremium = false),
                ToolMode("strobe", "Strobe", Icons.Default.AutoAwesome, isPremium = true),
                ToolMode("morse", "Morse", Icons.Default.GraphicEq, isPremium = true),
                ToolMode("colors", "Colors", Icons.Default.Palette, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "screen_torch",
            name = "Screen Light",
            description = "Full-screen soft panel light with color temperature & brightness swipe.",
            shortBenefit = "Soft full-screen panel light & colors",
            category = ToolCategory.LIGHT_AND_SOUND,
            icon = Icons.Default.Smartphone,
            isPro = false,
            heroGradientHex = 0xFF28283EL to 0xFF45456BL,
            keywords = listOf("screen", "light", "panel", "reading", "color"),
            modes = withMore(
                ToolMode("white", "Pure White", Icons.Default.Smartphone),
                ToolMode("warm", "Warm Amber", Icons.Default.Lightbulb),
                ToolMode("colors", "Colors", Icons.Default.Palette, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "strobe_torch",
            name = "Strobe Light",
            description = "1–20 Hz variable frequency LED & screen strobe flasher.",
            shortBenefit = "1–20 Hz variable frequency strobe",
            category = ToolCategory.LIGHT_AND_SOUND,
            icon = Icons.Default.Bolt,
            isPro = false,
            requiredPermission = Manifest.permission.CAMERA,
            requiredCapabilities = setOf(HardwareCapability.FLASH),
            heroGradientHex = 0xFF332214L to 0xFF5E3E24L,
            keywords = listOf("strobe", "flash", "blink", "beacon", "sos"),
            modes = withMore(
                ToolMode("strobe", "Strobe", Icons.Default.AutoAwesome, isPremium = true),
                ToolMode("sos", "SOS", Icons.Default.Bolt, isPremium = false),
                ToolMode("morse", "Morse", Icons.Default.GraphicEq, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "sound_db",
            name = "Sound Meter",
            description = "Approximate acoustic decibel (dB) gauge, graph, peak log, spectrum, reference levels & CSV export.",
            shortBenefit = "Live approximate dB meter, graph & CSV log",
            category = ToolCategory.LIGHT_AND_SOUND,
            icon = Icons.Default.GraphicEq,
            isPro = true,
            requiredPermission = Manifest.permission.RECORD_AUDIO,
            requiredCapabilities = setOf(HardwareCapability.MICROPHONE),
            heroGradientHex = 0xFF2C1E38L to 0xFF4E3463L,
            keywords = listOf("sound", "db", "decibel", "noise", "meter", "spectrum", "peak", "csv", "history"),
            modes = withMore(
                ToolMode("meter", "Meter", Icons.Default.GraphicEq, isPremium = true),
                ToolMode("graph", "Graph", Icons.Default.ShowChart, isPremium = true),
                ToolMode("peak_log", "Peak log", Icons.Default.BarChart, isPremium = true),
                ToolMode("spectrum", "Spectrum", Icons.Default.Equalizer, isPremium = true),
                ToolMode("reference", "Reference", Icons.AutoMirrored.Filled.MenuBook, isPremium = true),
                ToolMode("history_csv", "History & CSV", Icons.Default.History, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "magnifier_color",
            name = "Magnifier & Color",
            description = "Camera zoom magnifier with LED torch, freeze frame & live HEX/RGB/HSL color picker.",
            shortBenefit = "Camera magnifier + torch & HEX color picker",
            category = ToolCategory.LIGHT_AND_SOUND,
            icon = Icons.Default.ColorLens,
            isPro = false,
            requiredPermission = Manifest.permission.CAMERA,
            requiredCapabilities = setOf(HardwareCapability.CAMERA),
            heroGradientHex = 0xFF1C2C3BL to 0xFF314D68L,
            keywords = listOf("magnifier", "zoom", "loupe", "color picker", "hex", "rgb", "hsl", "camera", "torch"),
            modes = withMore(
                ToolMode("magnifier", "Magnifier", Icons.Default.CenterFocusStrong, isPremium = false),
                ToolMode("color_picker", "Color Picker", Icons.Default.ColorLens, isPremium = false),
                ToolMode("palette", "Saved Colors", Icons.Default.Palette, isPremium = false)
            )
        ),

        // ====================================================================
        // 4. TRAVEL & TIME
        // ====================================================================
        RegisteredTool(
            id = "pedometer",
            name = "Pedometer",
            description = "Approximate step counter with accelerometer fallback, daily goal ring, distance, estimated calories & history.",
            shortBenefit = "Daily steps, goal ring, distance & calories (approx.)",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.DirectionsRun,
            isPro = false,
            requiredPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else null,
            requiredCapabilities = setOf(HardwareCapability.STEP_COUNTER),
            heroGradientHex = 0xFF133329L to 0xFF215947L,
            keywords = listOf("pedometer", "steps", "step counter", "walk", "daily goal", "distance", "calories", "streak"),
            modes = withMore(
                ToolMode("steps", "Today's Steps", Icons.Default.DirectionsRun, isPremium = false),
                ToolMode("goal", "Daily Goal", Icons.Default.Star, isPremium = false),
                ToolMode("history", "Weekly & Monthly", Icons.Default.BarChart, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "pedometer_history",
            name = "Pedometer History",
            description = "Weekly and monthly approximate step charts, daily streaks, and custom step/distance/calorie goals.",
            shortBenefit = "Weekly/monthly step charts, streaks & goals",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.BarChart,
            isPro = true,
            requiredPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else null,
            requiredCapabilities = setOf(HardwareCapability.STEP_COUNTER),
            heroGradientHex = 0xFF1A362EL to 0xFF2D5E50L,
            keywords = listOf("pedometer history", "step chart", "weekly steps", "monthly steps", "streaks", "goals"),
            modes = withMore(
                ToolMode("history", "Charts & Streaks", Icons.Default.BarChart, isPremium = true),
                ToolMode("steps", "Live Pedometer", Icons.Default.DirectionsRun, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "qibla_finder",
            name = "Qibla Finder",
            description = "Approximate Qibla direction toward Mecca using magnetometer plus location with figure-8 calibration hint.",
            shortBenefit = "Approximate Qibla compass bearing & calibration",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Mosque,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.MAGNETOMETER),
            heroGradientHex = 0xFF12302AL to 0xFF21544AL,
            keywords = listOf("qibla", "mecca", "kaaba", "prayer", "compass", "direction", "calibration"),
            modes = withMore(
                ToolMode("qibla", "Qibla Dial", Icons.Default.Mosque, isPremium = false),
                ToolMode("calibration", "Calibration Hint", Icons.Default.CompassCalibration, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "parking_saver",
            name = "Parking Saver",
            description = "Save your parking spot with GPS coordinates, level/bay note, meter timer & live approximate arrow back.",
            shortBenefit = "Save parking spot with distance & arrow back",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Place,
            isPro = false,
            requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            heroGradientHex = 0xFF162C42L to 0xFF264C73L,
            keywords = listOf("parking", "car", "spot", "find my car", "location", "distance", "direction", "meter"),
            modes = withMore(
                ToolMode("spot", "Saved Spot", Icons.Default.Place, isPremium = false),
                ToolMode("return_arrow", "Direction Back", Icons.Default.Explore, isPremium = false),
                ToolMode("timer", "Meter Timer", Icons.Default.Timer, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "weather_calculators",
            name = "Weather Calc",
            description = "Approximate Heat Index, Dew Point & Wind Chill calculators from temperature, humidity & wind speed.",
            shortBenefit = "Approximate Heat Index, Dew Point & Wind Chill",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Thermostat,
            isPro = false,
            heroGradientHex = 0xFF182E3FL to 0xFF2B506EL,
            keywords = listOf("weather", "heat index", "dew point", "wind chill", "humidity", "temperature", "wind"),
            modes = withMore(
                ToolMode("heat_index", "Heat Index", Icons.Default.WbSunny, isPremium = false),
                ToolMode("dew_point", "Dew Point", Icons.Default.WaterDrop, isPremium = false),
                ToolMode("wind_chill", "Wind Chill", Icons.Default.Air, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "compass_waypoints",
            name = "Compass Waypoints",
            description = "Save up to 3 places for free (or unlimited in Pro) with a live approximate directional arrow & distance.",
            shortBenefit = "Up to 3 saved places with live direction arrow",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.MyLocation,
            isPro = false,
            requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION,
            requiredCapabilities = setOf(HardwareCapability.MAGNETOMETER, HardwareCapability.GPS),
            heroGradientHex = 0xFF142E3CL to 0xFF24526BL,
            keywords = listOf("waypoints", "compass", "places", "bearing", "distance", "arrow", "navigation"),
            modes = withMore(
                ToolMode("waypoints_free", "Waypoints (3 Free)", Icons.Default.Place, isPremium = false),
                ToolMode("unlimited", "Unlimited Waypoints", Icons.Default.MyLocation, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "waypoints_pro",
            name = "Unlimited Waypoints",
            description = "Save unlimited GPS waypoints with live approximate distance, true/magnetic bearing & directional arrow.",
            shortBenefit = "Unlimited waypoints with live distance & bearing",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.LocationOn,
            isPro = true,
            requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION,
            requiredCapabilities = setOf(HardwareCapability.MAGNETOMETER, HardwareCapability.GPS),
            heroGradientHex = 0xFF1C2D48L to 0xFF314F7DL,
            keywords = listOf("unlimited waypoints", "bearing", "distance", "gps", "coordinates", "navigation"),
            modes = withMore(
                ToolMode("unlimited", "Unlimited Waypoints", Icons.Default.LocationOn, isPremium = true),
                ToolMode("waypoints_free", "Quick 3 Waypoints", Icons.Default.Place, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "compass",
            name = "Compass",
            description = "Approximate True & Magnetic North rose dial, Sun/Moon direction, Qibla, coordinates, waypoints & CSV log.",
            shortBenefit = "True North, Sun/Moon, Qibla & Waypoints",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Explore,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.ACCELEROMETER, HardwareCapability.MAGNETOMETER),
            heroGradientHex = 0xFF102A42L to 0xFF1C4B75L,
            keywords = listOf("compass", "north", "heading", "qibla", "sun", "moon", "coordinates", "waypoints", "csv"),
            modes = withMore(
                ToolMode("compass", "Compass", Icons.Default.Explore, isPremium = false),
                ToolMode("sun_moon", "Sun & Moon direction", Icons.Default.WbSunny, isPremium = false),
                ToolMode("qibla", "Qibla", Icons.Default.Mosque, isPremium = false),
                ToolMode("coordinates", "Coordinates", Icons.Default.MyLocation, isPremium = false),
                ToolMode("waypoints", "Waypoints (3 Free)", Icons.Default.Place, isPremium = false),
                ToolMode("unlimited_waypoints", "Unlimited Waypoints", Icons.Default.LocationOn, isPremium = true),
                ToolMode("history_csv", "History & CSV", Icons.Default.History, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "speedometer",
            name = "Speedometer",
            description = "Live approximate GPS speed in km/h, mph & knots with Analog, Altitude, HUD & Trip log.",
            shortBenefit = "GPS digital/analog speed, HUD & trip log",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Speed,
            isPro = false,
            requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            heroGradientHex = 0xFF123038L to 0xFF1E5260L,
            keywords = listOf("speed", "gps", "kmh", "mph", "knots", "hud", "trip", "altitude", "analog"),
            modes = withMore(
                ToolMode("digital", "Digital", Icons.Default.Speed, isPremium = false),
                ToolMode("analog", "Analog", Icons.Default.AvTimer, isPremium = false),
                ToolMode("altitude", "Altitude", Icons.Default.Terrain, isPremium = false),
                ToolMode("hud", "HUD", Icons.Default.Flip, isPremium = true),
                ToolMode("trip", "Trip", Icons.Default.AltRoute, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "altimeter",
            name = "Altimeter",
            description = "Approximate barometric altitude & pressure trend (barometer) with a chart and QNH calibration.",
            shortBenefit = "Barometric altitude & pressure trend chart",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Terrain,
            isPro = true,
            requiredCapabilities = setOf(HardwareCapability.BAROMETER),
            heroGradientHex = 0xFF1C2E3DL to 0xFF30506BL,
            keywords = listOf("altitude", "elevation", "barometer", "mountain", "pressure", "trend", "chart", "qnh"),
            modes = withMore(
                ToolMode("altitude", "Elevation & Trend", Icons.Default.Terrain, isPremium = true),
                ToolMode("barometer", "Pressure Chart", Icons.Default.ShowChart, isPremium = true),
                ToolMode("qnh", "QNH Calibrate", Icons.Default.Tune, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "walk_run_tracker",
            name = "Walk / Run",
            description = "GPS route drawn on a Canvas, approximate distance, pace, splits, saved sessions & GPX/CSV export.",
            shortBenefit = "GPS route Canvas, pace, splits & GPX/CSV",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.DirectionsRun,
            isPro = true,
            requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION,
            requiredCapabilities = setOf(HardwareCapability.GPS),
            heroGradientHex = 0xFF163528L to 0xFF265C45L,
            keywords = listOf("walk", "run", "route", "pace", "splits", "gps", "gpx", "csv", "sessions"),
            modes = withMore(
                ToolMode("tracker", "Route & Pace", Icons.Default.DirectionsRun, isPremium = true),
                ToolMode("splits", "Splits", Icons.Default.Timer, isPremium = true),
                ToolMode("sessions", "Saved & Export", Icons.Default.Save, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "fishing_log",
            name = "Fishing Log",
            description = "Catch notes with time, GPS location, moon phase, barometric weather & CSV export.",
            shortBenefit = "Catch notes with location, moon phase & weather",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Phishing,
            isPro = true,
            heroGradientHex = 0xFF12323CL to 0xFF1F5768L,
            keywords = listOf("fish", "catch", "angling", "moon phase", "weather", "log", "csv"),
            modes = withMore(
                ToolMode("log", "Catch Log", Icons.Default.Phishing, isPremium = true),
                ToolMode("conditions", "Moon & Weather", Icons.Default.Cloud, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "world_clock",
            name = "World Clock",
            description = "Multi-zone live clock, Stopwatch, Timer, Meeting Planner & HIIT Interval.",
            shortBenefit = "World timezones, meeting planner & clocks",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Public,
            isPro = false,
            heroGradientHex = 0xFF162642L to 0xFF264273L,
            keywords = listOf("clock", "world", "timezone", "utc", "planner", "stopwatch", "timer", "interval"),
            modes = withMore(
                ToolMode("world", "World", Icons.Default.Public, isPremium = false),
                ToolMode("stopwatch", "Stopwatch", Icons.Default.Timer, isPremium = false),
                ToolMode("timer", "Timer", Icons.Default.HourglassBottom, isPremium = false),
                ToolMode("planner", "Planner", Icons.Default.Schedule, isPremium = false),
                ToolMode("interval", "Interval", Icons.Default.FitnessCenter, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "stopwatch",
            name = "Stopwatch & Timer",
            description = "Millisecond lap chronograph, Countdown Timer, World Clock, Planner & Interval.",
            shortBenefit = "Millisecond lap stopwatch & countdown timer",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Timer,
            isPro = false,
            heroGradientHex = 0xFF1E293BL to 0xFF334766L,
            keywords = listOf("stopwatch", "timer", "lap", "countdown", "chronograph", "interval"),
            modes = withMore(
                ToolMode("stopwatch", "Stopwatch", Icons.Default.Timer, isPremium = false),
                ToolMode("timer", "Timer", Icons.Default.HourglassBottom, isPremium = false),
                ToolMode("world", "World", Icons.Default.Public, isPremium = false),
                ToolMode("planner", "Planner", Icons.Default.Schedule, isPremium = false),
                ToolMode("interval", "Interval", Icons.Default.FitnessCenter, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "pomodoro_timer",
            name = "Pomodoro Focus",
            description = "25/5/15 work & break focus timer with audio chimes, cycle dots & streak stats.",
            shortBenefit = "Work/break focus cycles & sound alerts",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Spa,
            isPro = false,
            heroGradientHex = 0xFF2C1D2EL to 0xFF523456L,
            keywords = listOf("pomodoro", "focus", "study", "work", "break", "timer", "streak"),
            modes = withMore(
                ToolMode("focus", "Focus Timer", Icons.Default.Spa, isPremium = false),
                ToolMode("stats", "Stats & Streaks", Icons.Default.BarChart, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "pomodoro_stats",
            name = "Pomodoro Stats",
            description = "Daily focus streaks, completed Pomodoro sessions & 7-day productivity charts.",
            shortBenefit = "Focus statistics, daily streaks & charts",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.BarChart,
            isPro = true,
            heroGradientHex = 0xFF33202AL to 0xFF5E3B4DL,
            keywords = listOf("pomodoro", "statistics", "streaks", "productivity", "focus", "charts"),
            modes = withMore(
                ToolMode("stats", "Streaks & Charts", Icons.Default.BarChart, isPremium = true),
                ToolMode("focus", "Focus Timer", Icons.Default.Spa, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "interval_timer",
            name = "Interval Timer",
            description = "Configurable Work / Rest / Rounds HIIT, Tabata & boxing interval timer with presets.",
            shortBenefit = "Rounds, rest & HIIT/Tabata presets",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.HourglassBottom,
            isPro = true,
            heroGradientHex = 0xFF2B1E38L to 0xFF4B3463L,
            keywords = listOf("interval", "hiit", "tabata", "pomodoro", "rounds", "rest", "presets", "workout"),
            modes = withMore(
                ToolMode("interval", "Interval", Icons.Default.FitnessCenter, isPremium = true),
                ToolMode("presets", "Presets", Icons.Default.Star, isPremium = true),
                ToolMode("timer", "Timer", Icons.Default.HourglassBottom, isPremium = false),
                ToolMode("stopwatch", "Stopwatch", Icons.Default.Timer, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "tally_counter",
            name = "Tally Counter",
            description = "Multi-counter tally with haptic ticks, click sound & customizable target goals.",
            shortBenefit = "Named counters with haptic, sound & goal",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.TouchApp,
            isPro = false,
            heroGradientHex = 0xFF162E36L to 0xFF285361L,
            keywords = listOf("tally", "counter", "clicker", "count", "goal", "haptic", "laps"),
            modes = withMore(
                ToolMode("counters", "Counters", Icons.Default.TouchApp, isPremium = false),
                ToolMode("goals", "Target Goals", Icons.Default.Star, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "randomizer_suite",
            name = "Dice & Wheel",
            description = "Roll 1–6 dice, flip a coin, spin the pointer or customize the Decision Wheel.",
            shortBenefit = "Dice, Coin flip, Spinner & Decision Wheel",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.AutoAwesome,
            isPro = false,
            heroGradientHex = 0xFF241E3FL to 0xFF413673L,
            keywords = listOf("dice", "coin", "flip", "spinner", "decision wheel", "random", "roll", "picker"),
            modes = withMore(
                ToolMode("dice", "Dice", Icons.Default.GridView, isPremium = false),
                ToolMode("coin", "Coin Flip", Icons.Default.Flip, isPremium = false),
                ToolMode("spinner", "Spinner", Icons.Default.Explore, isPremium = false),
                ToolMode("wheel", "Decision Wheel", Icons.Default.AutoAwesome, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "fuel_trip_calc",
            name = "Fuel & Trip Cost",
            description = "Calculate trip fuel needed, total cost, cost per passenger & fuel economy.",
            shortBenefit = "Trip fuel cost, split & mileage calculator",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.LocalGasStation,
            isPro = false,
            heroGradientHex = 0xFF183028L to 0xFF2B5748L,
            keywords = listOf("fuel", "trip", "gas", "mileage", "mpg", "cost", "car", "roadtrip"),
            modes = withMore(
                ToolMode("trip_cost", "Trip Cost", Icons.Default.LocalGasStation, isPremium = false),
                ToolMode("mileage", "Fuel Economy", Icons.Default.Speed, isPremium = false),
                ToolMode("range", "Tank Range", Icons.Default.AltRoute, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "event_countdown",
            name = "Event Countdown",
            description = "Live days/hours/minutes countdowns for birthdays, trips & milestones with notifications.",
            shortBenefit = "Countdown & event timers with alerts",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Event,
            isPro = true,
            heroGradientHex = 0xFF2D1E38L to 0xFF523566L,
            keywords = listOf("countdown", "event", "birthday", "vacation", "timer", "notification", "days"),
            modes = withMore(
                ToolMode("events", "Countdowns", Icons.Default.Event, isPremium = true),
                ToolMode("add_event", "New Event", Icons.Default.CalendarMonth, isPremium = true),
                ToolMode("milestones", "Milestones", Icons.Default.Star, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "trip_gear_checklists",
            name = "Trip & Gear Lists",
            description = "Editable Camping, Fishing, Hunting & Travel packing templates with progress rings.",
            shortBenefit = "Camping, Fishing, Hunting & Travel gear lists",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Checklist,
            isPro = true,
            heroGradientHex = 0xFF193324L to 0xFF2D5C40L,
            keywords = listOf("trip", "gear", "checklist", "camping", "fishing", "hunting", "travel", "packing"),
            modes = withMore(
                ToolMode("camping", "Camping", Icons.Default.Terrain, isPremium = true),
                ToolMode("fishing", "Fishing", Icons.Default.Phishing, isPremium = true),
                ToolMode("hunting", "Hunting", Icons.Default.Explore, isPremium = true),
                ToolMode("travel", "Travel", Icons.Default.Public, isPremium = true)
            )
        ),

        // ====================================================================
        // 5. CONVERT & WRITE
        // ====================================================================
        RegisteredTool(
            id = "unit_converter",
            name = "Unit Converter",
            description = "13 categories (Length, Weight, Temp, Volume, Area, Speed, Time, Pressure, Energy, Data, Fuel, Angle, Currency) + Custom Units, Favorites & CSV Export.",
            shortBenefit = "Convert 13 unit categories offline",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.SwapHoriz,
            isPro = false,
            heroGradientHex = 0xFF231D42L to 0xFF3E3375L,
            keywords = listOf("convert", "unit", "length", "weight", "temperature", "volume", "area", "speed", "pressure", "energy", "data", "fuel", "currency", "custom"),
            modes = withMore(
                ToolMode("length", "Length", Icons.Default.Straighten),
                ToolMode("weight", "Weight", Icons.Default.Scale),
                ToolMode("temperature", "Temperature", Icons.Default.Thermostat),
                ToolMode("volume", "Volume", Icons.Default.WaterDrop),
                ToolMode("area", "Area", Icons.Default.SquareFoot),
                ToolMode("speed", "Speed", Icons.Default.Speed),
                ToolMode("time", "Time", Icons.Default.AccessTime),
                ToolMode("pressure", "Pressure", Icons.Default.Cloud),
                ToolMode("energy", "Energy", Icons.Default.Bolt),
                ToolMode("data", "Data", Icons.Default.Storage),
                ToolMode("fuel", "Fuel", Icons.Default.LocalGasStation),
                ToolMode("angle", "Angle", CustomToolIcons.AngleMeter),
                ToolMode("currency", "Currency", Icons.Default.AttachMoney),
                ToolMode("custom_units", "Custom Units", Icons.Default.Tune, isPremium = true),
                ToolMode("favorites", "Favorites", Icons.Default.Star, isPremium = true),
                ToolMode("history", "History & CSV", Icons.Default.History, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "unit_converter_pro",
            name = "Converter Extras",
            description = "Create custom conversion units, pin unlimited favorite pairs & export conversion history to CSV.",
            shortBenefit = "Custom units, favorites & CSV history export",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.Tune,
            isPro = true,
            heroGradientHex = 0xFF2D224CL to 0xFF4E3B82L,
            keywords = listOf("custom units", "converter extras", "favorites", "history export", "csv"),
            modes = withMore(
                ToolMode("custom_units", "Custom Units", Icons.Default.Tune, isPremium = true),
                ToolMode("favorites", "Favorites", Icons.Default.Star, isPremium = true),
                ToolMode("history", "History & CSV", Icons.Default.History, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "tip_calc",
            name = "Calculators",
            description = "Basic, EMI/Loan, Percent, Discount, GST/VAT, Tip-split, Date difference, Age & Scientific calculators.",
            shortBenefit = "Basic, EMI, Percent, Discount, GST, Tip, Date & Age",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.Calculate,
            isPro = false,
            heroGradientHex = 0xFF2A1D3DL to 0xFF4A336BL,
            keywords = listOf("calculator", "basic", "percent", "discount", "emi", "loan", "gst", "vat", "tax", "tip", "split", "date", "age", "scientific"),
            modes = withMore(
                ToolMode("basic", "Basic", Icons.Default.Calculate, isPremium = false),
                ToolMode("emi", "EMI / Loan", Icons.Default.AccountBalance, isPremium = false),
                ToolMode("percent", "Percent", Icons.Default.Percent, isPremium = false),
                ToolMode("discount", "Discount", Icons.Default.Receipt, isPremium = false),
                ToolMode("gst", "GST / VAT", Icons.Default.AttachMoney, isPremium = false),
                ToolMode("tip", "Tip Split", Icons.AutoMirrored.Filled.CallSplit, isPremium = false),
                ToolMode("date", "Date Diff", Icons.Default.CalendarMonth, isPremium = false),
                ToolMode("age", "Age", Icons.Default.Event, isPremium = false),
                ToolMode("scientific", "Scientific", Icons.Default.Functions, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "scientific_calc",
            name = "Scientific Calc",
            description = "Full trigonometric, logarithmic, factorial, power & parentheses solver with calculation history.",
            shortBenefit = "Scientific trig/log keypad with history tape",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.Functions,
            isPro = true,
            heroGradientHex = 0xFF321E46L to 0xFF58357AL,
            keywords = listOf("scientific", "calculator", "trig", "sin", "cos", "tan", "log", "history"),
            modes = withMore(
                ToolMode("scientific", "Scientific", Icons.Default.Functions, isPremium = true),
                ToolMode("history", "History Tape", Icons.Default.History, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "finance_pack",
            name = "Finance Pack",
            description = "Compound interest, SIP planner, side-by-side loan comparison, savings goal & currency rate alerts.",
            shortBenefit = "Compound, SIP, loan compare, goals & FX alerts",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.AccountBalance,
            isPro = true,
            heroGradientHex = 0xFF18332CL to 0xFF2A594DL,
            keywords = listOf("finance", "compound interest", "sip", "loan comparison", "savings goal", "currency", "rate alert"),
            modes = withMore(
                ToolMode("compound", "Compound", Icons.Default.BarChart, isPremium = true),
                ToolMode("sip", "SIP", Icons.Default.ShowChart, isPremium = true),
                ToolMode("loan_compare", "Loan Compare", Icons.Default.CompareArrows, isPremium = true),
                ToolMode("savings_goal", "Savings Goal", Icons.Default.Star, isPremium = true),
                ToolMode("currency_alerts", "Currency Alerts", Icons.Default.AttachMoney, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "split_bill_pro",
            name = "Split the Bill",
            description = "Split restaurant & group bills across multiple people and individual items with tax and tip.",
            shortBenefit = "Itemized multi-person bill & receipt splitter",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.AutoMirrored.Filled.CallSplit,
            isPro = true,
            heroGradientHex = 0xFF2E223CL to 0xFF533D6BL,
            keywords = listOf("split bill", "receipt", "items", "people", "friends", "dinner", "tip", "tax"),
            modes = withMore(
                ToolMode("itemized", "By Items", Icons.Default.Receipt, isPremium = true),
                ToolMode("equal", "Quick Split", Icons.AutoMirrored.Filled.CallSplit, isPremium = true),
                ToolMode("summary", "Shares", Icons.Default.BarChart, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "cooking_converter",
            name = "Cooking Converter",
            description = "Ingredient-aware cups, grams, ml, tbsp & oz converter plus oven temperatures & recipe scaler.",
            shortBenefit = "Convert cups, grams & ml by ingredient",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.Scale,
            isPro = false,
            heroGradientHex = 0xFF332418L to 0xFF5C402BL,
            keywords = listOf("cooking", "baking", "cups", "grams", "ml", "tablespoon", "teaspoon", "flour", "sugar", "butter", "oven"),
            modes = withMore(
                ToolMode("volume_weight", "Cups • Grams • ml", Icons.Default.Scale, isPremium = false),
                ToolMode("oven", "Oven Temp", Icons.Default.Thermostat, isPremium = false),
                ToolMode("scale", "Recipe Scaler", Icons.Default.Calculate, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "unit_price_comparer",
            name = "Unit Price",
            description = "Compare 2 or 3 products by price, weight or volume to spot the lowest unit cost and % savings.",
            shortBenefit = "Compare 2–3 items for best unit value",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.CompareArrows,
            isPro = false,
            heroGradientHex = 0xFF183032L to 0xFF2B5659L,
            keywords = listOf("unit price", "compare", "shopping", "grocery", "value", "cheaper", "deal"),
            modes = withMore(
                ToolMode("compare", "Compare 2–3", Icons.Default.CompareArrows, isPremium = false),
                ToolMode("bulk", "Bulk Savings", Icons.Default.Percent, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "password_generator",
            name = "Password Gen",
            description = "Offline SecureRandom password, memorable passphrase & PIN generator with entropy meter.",
            shortBenefit = "High-entropy offline password & PIN generator",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.Badge,
            isPro = false,
            heroGradientHex = 0xFF1A283BL to 0xFF2E4769L,
            keywords = listOf("password", "generator", "passphrase", "pin", "security", "random", "entropy"),
            modes = withMore(
                ToolMode("password", "Password", Icons.Default.Badge, isPremium = false),
                ToolMode("passphrase", "Passphrase", Icons.Default.EditNote, isPremium = false),
                ToolMode("pin", "PIN", Icons.Default.Calculate, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "text_tools",
            name = "Text Tools",
            description = "Word & character counter, case converter (UPPER, lower, Title, camel, snake) & Morse translator.",
            shortBenefit = "Word counter, case converter & Morse text",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.Description,
            isPro = false,
            heroGradientHex = 0xFF22243EL to 0xFF3D406EL,
            keywords = listOf("text", "word count", "character counter", "case converter", "uppercase", "lowercase", "morse"),
            modes = withMore(
                ToolMode("counter", "Word & Char", Icons.Default.Description, isPremium = false),
                ToolMode("case", "Case Converter", Icons.Default.EditNote, isPremium = false),
                ToolMode("morse", "Morse Translator", Icons.Default.GraphicEq, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "number_tools",
            name = "Number Tools",
            description = "Binary, Octal, Decimal & Hex base converter plus two-way Roman numerals & prime factors.",
            shortBenefit = "Base converter (Bin/Oct/Dec/Hex) & Roman numerals",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.Memory,
            isPro = false,
            heroGradientHex = 0xFF1E2B38L to 0xFF354C63L,
            keywords = listOf("number", "base converter", "binary", "hex", "octal", "decimal", "roman numerals"),
            modes = withMore(
                ToolMode("base", "Base Converter", Icons.Default.Memory, isPremium = false),
                ToolMode("roman", "Roman Numerals", Icons.Default.Functions, isPremium = false),
                ToolMode("factors", "Primes & GCD", Icons.Default.Calculate, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "qr_generator",
            name = "Scanner & QR",
            description = "Free plain QR generator (Wi-Fi, URL, text, contact) with saved history, plus QR/Barcode/Document scan.",
            shortBenefit = "Free Wi-Fi/URL/Text/Contact QR & History",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.QrCode2,
            isPro = false,
            heroGradientHex = 0xFF182C3CL to 0xFF2A4D69L,
            keywords = listOf("qr", "barcode", "scanner", "document", "id card", "generate", "history", "wifi", "contact", "url"),
            modes = withMore(
                ToolMode("generate_qr", "Plain QR (Free)", Icons.Default.QrCode2, isPremium = false),
                ToolMode("styles_pro", "Styles & Colors", Icons.Default.Palette, isPremium = true),
                ToolMode("history", "Saved QR History", Icons.Default.History, isPremium = false),
                ToolMode("qr", "Scan QR", Icons.Default.QrCodeScanner, isPremium = false),
                ToolMode("barcode", "Barcode", Icons.Default.CropFree, isPremium = false),
                ToolMode("document", "Document", Icons.Default.DocumentScanner, isPremium = false),
                ToolMode("id_card", "ID Card", Icons.Default.Badge, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "qr_studio_pro",
            name = "QR Studio Pro",
            description = "Custom QR styles, foreground/background color palettes, Wi-Fi/vCard templates & saved history.",
            shortBenefit = "Custom QR colors, module styles & history",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.Palette,
            isPro = true,
            heroGradientHex = 0xFF2A1E44L to 0xFF4C367AL,
            keywords = listOf("qr styles", "qr colors", "custom qr", "wifi qr", "vcard qr"),
            modes = withMore(
                ToolMode("styles_pro", "Styles & Colors", Icons.Default.Palette, isPremium = true),
                ToolMode("generate_qr", "Wi-Fi / URL / vCard", Icons.Default.QrCode2, isPremium = false),
                ToolMode("history", "Saved History", Icons.Default.History, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "image_toolkit",
            name = "Image Toolkit",
            description = "Compress, resize, convert JPG/PNG/WebP, strip EXIF & GPS location metadata, and batch process.",
            shortBenefit = "Compress, resize, JPG/PNG/WebP, EXIF & batch",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.CameraAlt,
            isPro = true,
            heroGradientHex = 0xFF2D1E36L to 0xFF523663L,
            keywords = listOf("image", "compress", "resize", "convert", "jpg", "png", "webp", "exif", "metadata", "batch"),
            modes = withMore(
                ToolMode("compress", "Compress", Icons.Default.Save, isPremium = true),
                ToolMode("resize", "Resize", Icons.Default.CropFree, isPremium = true),
                ToolMode("convert", "JPG/PNG/WebP", Icons.Default.SwapHoriz, isPremium = true),
                ToolMode("exif", "Remove EXIF", Icons.Default.LocationOn, isPremium = true),
                ToolMode("batch", "Batch", Icons.Default.GridView, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "quick_note",
            name = "Notes",
            description = "Text notes, interactive checklists, freehand drawings & note templates.",
            shortBenefit = "Text, checklists, sketches & templates",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.EditNote,
            isPro = false,
            heroGradientHex = 0xFF28203CL to 0xFF47396BL,
            keywords = listOf("note", "text", "checklist", "todo", "drawing", "sketch", "templates"),
            modes = withMore(
                ToolMode("text", "Text", Icons.Default.EditNote, isPremium = false),
                ToolMode("checklist", "Checklist", Icons.Default.Checklist, isPremium = false),
                ToolMode("drawing", "Drawing", Icons.Default.Brush, isPremium = false),
                ToolMode("templates", "Templates", Icons.Default.Description, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "pdf_tools",
            name = "PDF Tools",
            description = "Merge, split, reorder, rotate, delete, page numbers, watermark, compress, Images to PDF, PDF to Images & Lock.",
            shortBenefit = "Merge, split, compress, watermark & convert PDFs",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.PictureAsPdf,
            isPro = false,
            heroGradientHex = 0xFF381A28L to 0xFF662E47L,
            keywords = listOf("pdf", "merge", "split", "compress", "watermark", "page numbers", "images to pdf", "pdf to images", "lock"),
            modes = withMore(
                ToolMode("studio", "PDF Studio", Icons.Default.PictureAsPdf),
                ToolMode("merge_split", "Merge & Split", Icons.AutoMirrored.Filled.CallSplit),
                ToolMode("compress", "Compress & Lock", Icons.Default.Save)
            )
        ),
        RegisteredTool(
            id = "device_sensors",
            name = "Device & Sensors",
            description = "Live telemetry for all hardware sensors plus RAM, storage, battery & screen specs.",
            shortBenefit = "Live hardware sensors & phone specs",
            category = ToolCategory.CONVERT_AND_WRITE,
            icon = Icons.Default.Sensors,
            isPro = false,
            heroGradientHex = 0xFF162C38L to 0xFF264D63L,
            keywords = listOf("device", "sensors", "hardware", "ram", "storage", "specs", "telemetry"),
            modes = withMore(
                ToolMode("overview", "All Specs", Icons.Default.Sensors),
                ToolMode("hardware", "System Info", Icons.Default.Memory)
            )
        ),

        // ====================================================================
        // 6. MEDIA STUDIO
        // ====================================================================
        RegisteredTool(
            id = "photo_converter",
            name = "Photo Converter",
            description = "Convert JPG, PNG, WebP, HEIC/HEIF, BMP, GIF & AVIF with live size estimate, resize (%, px, 1080p/2K/4K), EXIF toggle & rename pattern.",
            shortBenefit = "JPG/PNG/WebP/HEIC converter & resizer",
            category = ToolCategory.MEDIA_STUDIO,
            icon = Icons.Default.SwapHoriz,
            isPro = false,
            heroGradientHex = 0xFF3A162EL to 0xFF6B2854L,
            keywords = listOf("photo converter", "image converter", "heic", "heif", "webp", "avif", "jpg", "png", "bmp", "gif", "resize", "exif", "batch"),
            modes = withMore(
                ToolMode("single", "Single (≤12 MP)", Icons.Default.SwapHoriz, isPremium = false),
                ToolMode("resize_exif", "Resize & EXIF", Icons.Default.CropFree, isPremium = false),
                ToolMode("batch", "Batch (Up to 50)", Icons.Default.GridView, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "photo_converter_pro",
            name = "Batch Photo Converter",
            description = "Batch convert up to 50 images at full camera resolution with custom rename pattern, EXIF control & 4K presets.",
            shortBenefit = "Batch convert up to 50 photos at full res",
            category = ToolCategory.MEDIA_STUDIO,
            icon = Icons.Default.GridView,
            isPro = true,
            heroGradientHex = 0xFF421834L to 0xFF7A2D60L,
            keywords = listOf("batch photo converter", "bulk convert", "full resolution", "50 photos", "heic to jpg"),
            modes = withMore(
                ToolMode("batch", "Batch (Up to 50)", Icons.Default.GridView, isPremium = true),
                ToolMode("single", "Single Photo", Icons.Default.SwapHoriz, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "background_remover",
            name = "Background Remover",
            description = "On-device background cutout with checkerboard preview, refine brush, feather, backdrop swap, shadow & PNG/JPG export.",
            shortBenefit = "On-device AI cutout, refine brush & backdrop",
            category = ToolCategory.MEDIA_STUDIO,
            icon = Icons.Default.AutoAwesome,
            isPro = false,
            requiredCapabilities = setOf(HardwareCapability.MLKIT_SEGMENTATION),
            heroGradientHex = 0xFF1F163EL to 0xFF3B2A75L,
            keywords = listOf("background remover", "cutout", "transparent png", "segmentation", "erase background", "backdrop", "feather"),
            modes = withMore(
                ToolMode("cutout", "AI Cutout", Icons.Default.AutoAwesome, isPremium = false),
                ToolMode("refine", "Refine Brush", Icons.Default.Brush, isPremium = false),
                ToolMode("backdrop", "New Background", Icons.Default.Palette, isPremium = false),
                ToolMode("hd_export", "HD Export", Icons.Default.Save, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "photo_editor",
            name = "Photo Editor",
            description = "Crop, rotate, adjust, 20 ColorMatrix filters, curves/grain/fade, draw & mosaic brush, text, stickers, frames, 2–6 collage & meme maker.",
            shortBenefit = "Crop, 20 filters, draw, meme & collage studio",
            category = ToolCategory.MEDIA_STUDIO,
            icon = Icons.Default.Brush,
            isPro = false,
            heroGradientHex = 0xFF162B3EL to 0xFF284F73L,
            keywords = listOf("photo editor", "crop", "filters", "adjust", "curves", "grain", "draw", "mosaic", "text", "stickers", "frames", "collage", "meme"),
            modes = withMore(
                ToolMode("crop_adjust", "Crop & Adjust", Icons.Default.CropFree, isPremium = false),
                ToolMode("filters", "20 Filters", Icons.Default.ColorLens, isPremium = false),
                ToolMode("draw_text", "Draw, Text & Stickers", Icons.Default.Brush, isPremium = false),
                ToolMode("meme", "Meme & Frames", Icons.Default.Star, isPremium = false),
                ToolMode("curves_grain", "Curves & Grain", Icons.Default.ShowChart, isPremium = true),
                ToolMode("collage", "Collage (2–6)", Icons.Default.GridView, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "passport_id_photo",
            name = "Passport & ID Photo",
            description = "US 2×2 in, India/UK/EU 35×45 mm, Canada 50×70 mm & China 33×48 mm with face guide, white/blue background & 300 DPI 4×6 in / A4 print sheet.",
            shortBenefit = "Official ID sizes + 300 DPI 4×6 & A4 print sheet",
            category = ToolCategory.MEDIA_STUDIO,
            icon = Icons.Default.Badge,
            isPro = true,
            heroGradientHex = 0xFF1A2848L to 0xFF2E4780L,
            keywords = listOf("passport photo", "id photo", "visa photo", "2x2", "35x45", "print sheet", "300 dpi", "biometric photo"),
            modes = withMore(
                ToolMode("single_id", "ID & Passport Crop", Icons.Default.Badge, isPremium = true),
                ToolMode("print_sheet", "4×6 / A4 Print Sheet", Icons.Default.GridView, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "audio_converter",
            name = "Audio Converter",
            description = "Input MP3, M4A/AAC, WAV, OGG, FLAC, AMR & 3GP; output WAV, M4A (AAC) & device FLAC/Opus with waveform cutter, ringtone maker, merge & speed/pitch.",
            shortBenefit = "Audio converter, cutter, ringtone & merge",
            category = ToolCategory.MEDIA_STUDIO,
            icon = Icons.Default.LibraryMusic,
            isPro = false,
            heroGradientHex = 0xFF143330L to 0xFF235C56L,
            keywords = listOf("audio converter", "wav", "m4a", "aac", "flac", "opus", "mp3 input", "audio cutter", "ringtone maker", "merge audio", "speed", "pitch", "extract audio"),
            modes = withMore(
                ToolMode("convert", "Convert Audio", Icons.Default.LibraryMusic, isPremium = false),
                ToolMode("cutter_ringtone", "Cutter & Ringtone", Icons.Default.GraphicEq, isPremium = false),
                ToolMode("extract_video", "Extract from Video", Icons.Default.MusicNote, isPremium = false),
                ToolMode("merge", "Merge Audio", Icons.AutoMirrored.Filled.CallSplit, isPremium = true),
                ToolMode("speed_pitch", "Speed & Pitch", Icons.Default.Speed, isPremium = true),
                ToolMode("batch_unlimited", "Batch & >5 Min", Icons.Default.Storage, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "video_tools",
            name = "Video Tools",
            description = "Lossless video trim, extract audio & mute video (MediaExtractor/MediaMuxer) plus Media3 hardware compress & resize.",
            shortBenefit = "Trim, extract audio, mute & compress video",
            category = ToolCategory.MEDIA_STUDIO,
            icon = Icons.Default.SurroundSound,
            isPro = false,
            heroGradientHex = 0xFF2E1A36L to 0xFF543063L,
            keywords = listOf("video tools", "trim video", "mute video", "extract audio", "compress video", "resize video", "media3"),
            modes = withMore(
                ToolMode("trim", "Trim Video", Icons.Default.Timeline, isPremium = false),
                ToolMode("mute", "Mute Video", Icons.AutoMirrored.Filled.VolumeUp, isPremium = false),
                ToolMode("extract_audio", "Extract Audio", Icons.Default.MusicNote, isPremium = false),
                ToolMode("compress_resize", "Compress & Resize", Icons.Default.Speed, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "video_compress_pro",
            name = "Video Compressor",
            description = "Compress and resize videos (1080p, 720p, 480p, bitrate target) on-device using AndroidX Media3 Transformer.",
            shortBenefit = "Hardware video compress & resolution scaler",
            category = ToolCategory.MEDIA_STUDIO,
            icon = Icons.Default.Speed,
            isPro = true,
            requiredCapabilities = setOf(HardwareCapability.MEDIA3_TRANSFORMER),
            heroGradientHex = 0xFF361A36L to 0xFF632E63L,
            keywords = listOf("video compressor", "resize video", "media3 transformer", "shrink video", "1080p", "720p"),
            modes = withMore(
                ToolMode("compress_resize", "Compress & Resize", Icons.Default.Speed, isPremium = true),
                ToolMode("trim", "Trim Video", Icons.Default.Timeline, isPremium = false)
            )
        ),
        RegisteredTool(
            id = "pdf_image_tools",
            name = "PDF & Image Studio",
            description = "Images to PDF, PDF to JPG/PNG, bulk image compress, EXIF viewer/cleaner, Base64 codec, photo color palette & social resize presets.",
            shortBenefit = "PDF/Image converter, EXIF cleaner, palette & Base64",
            category = ToolCategory.MEDIA_STUDIO,
            icon = Icons.Default.PictureAsPdf,
            isPro = false,
            heroGradientHex = 0xFF331B24L to 0xFF5E3142L,
            keywords = listOf("images to pdf", "pdf to jpg", "pdf to png", "bulk image compress", "exif cleaner", "base64", "color palette", "social resize"),
            modes = withMore(
                ToolMode("img_to_pdf", "Images to PDF", Icons.Default.PictureAsPdf, isPremium = false),
                ToolMode("pdf_to_img", "PDF to JPG/PNG", Icons.Default.Description, isPremium = false),
                ToolMode("exif_clean", "EXIF Viewer & Clean", Icons.Default.CameraAlt, isPremium = false),
                ToolMode("palette", "Photo Palette", Icons.Default.Palette, isPremium = false),
                ToolMode("social_resize", "Social Presets", Icons.Default.CropFree, isPremium = false),
                ToolMode("base64", "Base64 Codec", Icons.Default.Memory, isPremium = false),
                ToolMode("bulk_compress", "Bulk Compress (Pro)", Icons.Default.Storage, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "outdoor_planner",
            name = "Outdoor Planner",
            description = "Rank the next 7 days for Fishing, Hunting, or Photography with a tide-free Best Day card, Air Quality & UV, and shareable day card image.",
            shortBenefit = "7-day Fishing, Hunting & Photo ranker + Best Day card",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Explore,
            isPro = true,
            heroGradientHex = 0xFF102A28L to 0xFF1D4F4AL,
            keywords = listOf("outdoor planner", "best day", "fishing", "hunting", "photography", "aqi", "uv index", "shareable card"),
            modes = withMore(
                ToolMode("fishing", "Fishing Rank", Icons.Default.WaterDrop, isPremium = true),
                ToolMode("hunting", "Hunting Rank", Icons.Default.Explore, isPremium = true),
                ToolMode("photography", "Photo Rank", Icons.Default.CameraAlt, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "sun_moon_calendar",
            name = "Sun & Moon Calendar",
            description = "Monthly Sun & Moon calendar with moon phases, best Solunar days, interactive sky dome with sun path, golden & blue hour.",
            shortBenefit = "Monthly moon phases, Solunar best days & interactive sky dome",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.DarkMode,
            isPro = true,
            heroGradientHex = 0xFF171C36L to 0xFF2B3566L,
            keywords = listOf("sun moon calendar", "moon phase", "lunar calendar", "sky dome", "golden hour", "blue hour", "solunar"),
            modes = withMore(
                ToolMode("calendar", "Monthly Calendar", Icons.Default.CalendarMonth, isPremium = true),
                ToolMode("sky_dome", "Interactive Sky Dome", Icons.Default.WbSunny, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "outdoor_alerts",
            name = "Outdoor Alerts",
            description = "On-device WorkManager alerts for Solunar best windows (Fishing/Hunting, 15/30/60m lead), golden hour, daily weather summary, and rain-soon alerts.",
            shortBenefit = "Solunar, golden-hour, daily weather & rain-soon notifications",
            category = ToolCategory.TRAVEL_AND_TIME,
            icon = Icons.Default.Notifications,
            isPro = true,
            heroGradientHex = 0xFF2B2112L to 0xFF543F1EL,
            keywords = listOf("alerts", "notifications", "solunar alert", "golden hour alert", "rain alert", "weather summary", "workmanager"),
            modes = withMore(
                ToolMode("solunar", "Solunar Alerts", Icons.Default.Waves, isPremium = true),
                ToolMode("golden_hour", "Golden Hour", Icons.Default.WbSunny, isPremium = true),
                ToolMode("weather", "Weather & Rain", Icons.Default.Cloud, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "home_widgets",
            name = "Home Widgets",
            description = "Jetpack Glance home screen widgets for Flashlight toggle, Compass heading, Solunar score, Quick Scan, and Weather.",
            shortBenefit = "Glance widgets: Torch, Compass, Solunar, Scan & Weather",
            category = ToolCategory.PHONE_CARE_AND_AUDIO,
            icon = Icons.Default.GridView,
            isPro = true,
            heroGradientHex = 0xFF162436L to 0xFF284363L,
            keywords = listOf("widgets", "home screen widgets", "glance", "flashlight widget", "solunar widget", "weather widget", "quick scan widget", "compass widget"),
            modes = withMore(
                ToolMode("gallery", "Widget Gallery", Icons.Default.GridView, isPremium = true)
            )
        ),
        RegisteredTool(
            id = "custom_style_studio",
            name = "Style & Accent Studio",
            description = "Custom hex accent color picker, Glass/Solid/Outline tile styles, hero carousel styles, icon sets, and 6 extra Pro themes.",
            shortBenefit = "Custom accent color, tile styles, carousel & icon sets",
            category = ToolCategory.LIGHT_AND_SOUND,
            icon = Icons.Default.Palette,
            isPro = true,
            heroGradientHex = 0xFF281736L to 0xFF4C2B66L,
            keywords = listOf("customization", "accent color", "tile style", "hero carousel", "icon set", "themes", "glass", "solid", "outline"),
            modes = withMore(
                ToolMode("accent", "Accent Color", Icons.Default.Palette, isPremium = true),
                ToolMode("tiles", "Tile & Carousel", Icons.Default.GridView, isPremium = true),
                ToolMode("icons", "Icon Sets", Icons.Default.AutoAwesome, isPremium = true)
            )
        )
    )

    val ALL_TOOLS: List<RegisteredTool>
        get() = _tools.value

    val DEFAULT_ENABLED_IDS: Set<String> = defaultToolsList.map { it.id }.toSet()

    private val _tools = MutableStateFlow(defaultToolsList)
    val tools: StateFlow<List<RegisteredTool>> = _tools.asStateFlow()

    private val _orderedCategories = MutableStateFlow(ToolCategory.entries.toList())
    val orderedCategories: StateFlow<List<ToolCategory>> = _orderedCategories.asStateFlow()

    /**
     * Maps legacy or alias tool IDs so links like `"bubble_level"`, `"angle_meter"`, `"metal_detector"`,
     * `"sound_meter"`, `"calculator"`, `"checklist"` resolve cleanly to the registered tool.
     */
    private fun normalizeToolId(rawId: String): String {
        val clean = rawId.substringBefore(":").trim().removePrefix("feature/")
        return when (clean) {
            "bubble_level" -> "level"
            "angle_meter", "clinometer", "protractor" -> "angle"
            "metal_detector" -> "metal_finder"
            "sound_meter" -> "sound_db"
            "calculator", "basic_calculator", "emi_calculator", "tip_split", "percentage", "date_diff", "age_calculator", "calculators" -> "tip_calc"
            "finance_calculators" -> "finance_pack"
            "split_bill" -> "split_bill_pro"
            "fuel_calculator" -> "fuel_trip_calc"
            "unit_price" -> "unit_price_comparer"
            "number_converter" -> "number_tools"
            "checklist" -> "quick_note"
            "screen_ruler" -> "ruler"
            "dice", "coin_flip", "spinner", "decision_wheel", "randomizer" -> "randomizer_suite"
            "step_counter", "steps" -> "pedometer"
            "qibla" -> "qibla_finder"
            "parking", "find_my_car" -> "parking_saver"
            "heat_index", "dew_point", "wind_chill" -> "weather_calculators"
            "unlimited_waypoints" -> "waypoints_pro"
            "background_remover_pro" -> "background_remover"
            "photo_editor_pro", "photo_collage_pro" -> "photo_editor"
            "audio_converter_pro", "audio_studio_pro" -> "audio_converter"
            "pdf_image_pro", "pdf_image_studio", "pdf_tools", "exif_viewer" -> "pdf_image_tools"
            "passport_photo" -> "passport_id_photo"
            "tone_generator", "frequency_generator", "frequency_presets" -> "frequency_tool"
            "musical_tones" -> "tone_generator"
            "noise_generator" -> "noise_sleep_mixer"
            "morse_code" -> "text_tools"
            "vibration_test" -> "vibration_lab"
            "screen_pixel_test" -> "screen_test"
            "device_info" -> "device_sensors"
            "magnifier", "color_picker" -> "magnifier_color"
            "light_meter" -> "lux_meter"
            "vibration_meter" -> "vibration_gforce"
            "trip_checklists" -> "trip_gear_checklists"
            "pomodoro" -> "pomodoro_timer"
            "open_qr_scanner", "open_doc_scanner" -> "qr_generator_styles"
            "open_outdoors_solunar" -> "fishing_log"
            "open_outdoors_sun_moon" -> "sun_moon_calendar"
            "open_outdoors_weather" -> "weather_calculators"
            "open_library_notes" -> "quick_note"
            else -> {
                if (defaultToolsList.any { it.id == clean }) {
                    clean
                } else {
                    val catalogFeature = FeatureRegistry.getFeatureById(clean)
                    if (catalogFeature != null && catalogFeature.toolId != clean) {
                        normalizeToolId(catalogFeature.toolId)
                    } else {
                        clean
                    }
                }
            }
        }
    }

    fun getToolById(id: String): RegisteredTool? {
        val norm = normalizeToolId(id)
        return _tools.value.find { it.id == norm } ?: defaultToolsList.find { it.id == norm }
    }

    fun getById(id: String): RegisteredTool? = getToolById(id)

    /**
     * Resolves a favorite/recent entry which may be either `"toolId"` or `"toolId:modeId"`.
     */
    fun resolveToolAndMode(entry: String): Pair<RegisteredTool, ToolMode?>? {
        val trimmed = entry.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split(":")
        val tool = getToolById(parts[0].trim()) ?: return null
        val mode = if (parts.size >= 2) {
            val modeId = parts[1].trim()
            tool.modes.find { it.id.equals(modeId, ignoreCase = true) }
        } else null
        return tool to mode
    }

    /**
     * Returns the ordered & visible modes for [tool], applying any user overrides from
     * `customModesConfigCsv` (format: `"toolId=mode1:1,mode2:0,mode3:1;otherTool=..."`).
     * Always keeps at least one non-More mode enabled and appends the `"more"` mode at the end.
     */
    fun getEffectiveModesForTool(tool: RegisteredTool, customModesConfigCsv: String = ""): List<ToolMode> {
        if (tool.modes.isEmpty()) return listOf(MORE_MODE)
        if (customModesConfigCsv.isBlank()) return tool.modes

        val toolConfigEntry = customModesConfigCsv.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("${tool.id}=", ignoreCase = true) }
            ?: return tool.modes

        val specs = toolConfigEntry.substringAfter("=").split(",").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val mId = parts[0].trim()
                val enabled = parts.getOrNull(1)?.trim() != "0"
                mId to enabled
            } else null
        }
        if (specs.isEmpty()) return tool.modes

        val byId = tool.modes.filter { it.id != "more" }.associateBy { it.id }
        val ordered = mutableListOf<ToolMode>()
        val hiddenIds = mutableSetOf<String>()

        for ((mId, enabled) in specs) {
            val m = byId[mId] ?: continue
            if (enabled) {
                if (m !in ordered) ordered.add(m)
            } else {
                hiddenIds.add(mId)
            }
        }
        for (m in tool.modes) {
            if (m.id != "more" && m.id !in hiddenIds && m !in ordered) {
                ordered.add(m)
            }
        }
        if (ordered.isEmpty()) {
            ordered.add(tool.modes.first { it.id != "more" })
        }
        ordered.add(MORE_MODE)
        return ordered
    }

    /**
     * Returns all non-"More" modes for [tool] paired with their enabled state for Customize Home.
     */
    fun getConfigurableModesForTool(
        tool: RegisteredTool,
        customModesConfigCsv: String = ""
    ): List<Pair<ToolMode, Boolean>> {
        val baseModes = tool.modes.filter { it.id != "more" }
        if (baseModes.isEmpty()) return emptyList()
        if (customModesConfigCsv.isBlank()) return baseModes.map { it to true }

        val toolConfigEntry = customModesConfigCsv.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("${tool.id}=", ignoreCase = true) }
            ?: return baseModes.map { it to true }

        val specs = toolConfigEntry.substringAfter("=").split(",").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val mId = parts[0].trim()
                val enabled = parts.getOrNull(1)?.trim() != "0"
                mId to enabled
            } else null
        }
        if (specs.isEmpty()) return baseModes.map { it to true }

        val byId = baseModes.associateBy { it.id }
        val result = mutableListOf<Pair<ToolMode, Boolean>>()
        val seen = mutableSetOf<String>()
        for ((mId, enabled) in specs) {
            val m = byId[mId] ?: continue
            if (seen.add(mId)) {
                result.add(m to enabled)
            }
        }
        for (m in baseModes) {
            if (seen.add(m.id)) {
                result.add(m to true)
            }
        }
        return result
    }

    /**
     * Searches both tools and individual tool modes so queries like "Strobe", "Qibla", "HUD",
     * or "EMI" return `"Flashlight > Strobe"`, `"Compass > Qibla"`, etc.
     */
    fun searchToolsAndModes(
        query: String,
        tools: List<RegisteredTool> = _tools.value,
        customModesConfigCsv: String = ""
    ): List<ToolOrModeSearchResult> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            return tools.map { ToolOrModeSearchResult(tool = it) }
        }
        val results = mutableListOf<ToolOrModeSearchResult>()
        for (tool in tools) {
            val toolMatches = tool.name.lowercase().contains(q) ||
                tool.description.lowercase().contains(q) ||
                tool.shortBenefit.lowercase().contains(q) ||
                tool.category.title.lowercase().contains(q) ||
                tool.keywords.any { it.lowercase().contains(q) }
            if (toolMatches) {
                results.add(ToolOrModeSearchResult(tool = tool, mode = null))
            }
            val effectiveModes = getEffectiveModesForTool(tool, customModesConfigCsv)
            for (mode in effectiveModes) {
                if (mode.id == "more") continue
                val fullLabel = "${tool.name} > ${mode.title}".lowercase()
                if (mode.title.lowercase().contains(q) || mode.id.lowercase().contains(q) || fullLabel.contains(q)) {
                    results.add(
                        ToolOrModeSearchResult(
                            tool = tool,
                            mode = mode,
                            displayTitle = "${tool.name} > ${mode.title}",
                            subtitle = "${tool.name} mode • ${tool.category.title}"
                        )
                    )
                }
            }
        }
        return results
    }

    fun getRelatedTools(tool: RegisteredTool): List<RegisteredTool> {
        return _tools.value.filter { it.category == tool.category && it.id != tool.id && it.isEnabled }
    }

    fun syncFromSavedCsv(
        enabledIdsCsv: String,
        orderedIdsCsv: String,
        orderedSectionsCsv: String
    ) {
        val enabledSet: Set<String>? = if (enabledIdsCsv.isBlank()) {
            null
        } else {
            enabledIdsCsv.split(",").map { normalizeToolId(it) }.filter { it.isNotEmpty() }.toSet()
        }

        val orderedIds = if (orderedIdsCsv.isBlank()) {
            defaultToolsList.map { it.id }
        } else {
            val parsed = orderedIdsCsv.split(",")
                .map { normalizeToolId(it) }
                .filter { it.isNotEmpty() }
                .distinct()
                .toMutableList()
            defaultToolsList.forEach { t -> if (t.id !in parsed) parsed.add(t.id) }
            parsed
        }

        val baseMap = defaultToolsList.associateBy { it.id }
        _tools.value = orderedIds.mapNotNull { id ->
            val base = baseMap[id] ?: return@mapNotNull null
            base.copy(isEnabled = enabledSet?.contains(id) ?: true)
        }

        if (orderedSectionsCsv.isNotBlank()) {
            val cats = orderedSectionsCsv.split(",")
                .mapNotNull { name -> runCatching { ToolCategory.valueOf(name.trim()) }.getOrNull() }
                .distinct()
                .toMutableList()
            ToolCategory.entries.forEach { c -> if (c !in cats) cats.add(c) }
            _orderedCategories.value = cats
        }
    }

    fun applyPreset(preset: HomeNeedPreset) {
        _tools.value = _tools.value.map { tool ->
            tool.copy(isEnabled = preset.toolIds.contains(tool.id))
        }
    }

    fun setToolEnabled(toolId: String, enabled: Boolean) {
        val norm = normalizeToolId(toolId)
        _tools.value = _tools.value.map { tool ->
            if (tool.id == norm) tool.copy(isEnabled = enabled) else tool
        }
    }

    fun moveTool(fromIndex: Int, toIndex: Int) {
        val current = _tools.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _tools.value = current
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        val current = _orderedCategories.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _orderedCategories.value = current
    }

    fun resetToDefault() {
        _tools.value = defaultToolsList
        _orderedCategories.value = ToolCategory.entries.toList()
    }
}
