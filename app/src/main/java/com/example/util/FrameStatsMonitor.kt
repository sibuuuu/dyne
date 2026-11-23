package com.example.util

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

@Immutable
data class ScreenFrameStats(
    val screenName: String,
    val totalFrames: Long = 0L,
    val jankyFrames: Long = 0L,
    val maxFrameMs: Double = 0.0,
    val sumFrameMs: Double = 0.0
) {
    val jankPercent: Double
        get() = if (totalFrames <= 0L) 0.0 else (jankyFrames.toDouble() * 100.0) / totalFrames.toDouble()

    val avgFrameMs: Double
        get() = if (totalFrames <= 0L) 0.0 else sumFrameMs / totalFrames.toDouble()

    val formattedJankPercent: String
        get() = String.format(Locale.US, "%.1f%%", jankPercent)

    val formattedAvgFrameMs: String
        get() = String.format(Locale.US, "%.1f ms", avgFrameMs)
}

/**
 * Lightweight per-screen frame & jank tracker (JankStats / Window.OnFrameMetricsAvailableListener).
 * Tracks total frames, janky frames (>16.67 ms), and jank percentage per screen without setting
 * any display refresh-rate overrides.
 */
object FrameStatsMonitor {

    private const val JANK_THRESHOLD_NS = 16_666_667L // 16.67ms (60fps frame budget)

    private val defaultScreenOrder = listOf(
        "Home",
        "Outdoors",
        "Library",
        "Settings",
        "All Features",
        "Tool Screen"
    )

    @Volatile
    var currentScreenName: String = "Home"
        private set

    private val _statsByScreen = MutableStateFlow<Map<String, ScreenFrameStats>>(
        defaultScreenOrder.associateWith { ScreenFrameStats(screenName = it) }
    )
    val statsByScreen: StateFlow<Map<String, ScreenFrameStats>> = _statsByScreen.asStateFlow()

    fun setCurrentScreen(screenName: String) {
        val normalized = screenName.ifBlank { "Home" }
        currentScreenName = normalized
        _statsByScreen.update { current ->
            if (current.containsKey(normalized)) {
                current
            } else {
                current + (normalized to ScreenFrameStats(screenName = normalized))
            }
        }
    }

    fun recordFrameDurationNs(durationNs: Long, screenName: String = currentScreenName) {
        if (durationNs <= 0L) return
        val durationMs = durationNs.toDouble() / 1_000_000.0
        val isJank = durationNs > JANK_THRESHOLD_NS
        _statsByScreen.update { current ->
            val prev = current[screenName] ?: ScreenFrameStats(screenName = screenName)
            val updated = prev.copy(
                totalFrames = prev.totalFrames + 1L,
                jankyFrames = prev.jankyFrames + if (isJank) 1L else 0L,
                maxFrameMs = maxOf(prev.maxFrameMs, durationMs),
                sumFrameMs = prev.sumFrameMs + durationMs
            )
            current + (screenName to updated)
        }
    }

    fun reset() {
        _statsByScreen.value = defaultScreenOrder.associateWith { ScreenFrameStats(screenName = it) }
    }
}

@Composable
fun AttachFrameStatsObserver(currentScreenName: String) {
    val context = LocalContext.current
    DisposableEffect(currentScreenName) {
        FrameStatsMonitor.setCurrentScreen(currentScreenName)
        onDispose { }
    }

    DisposableEffect(context) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val handler = Handler(Looper.getMainLooper())
            val listener = Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
                val totalDurationNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                FrameStatsMonitor.recordFrameDurationNs(totalDurationNs)
            }
            runCatching {
                window.addOnFrameMetricsAvailableListener(listener, handler)
            }
            onDispose {
                runCatching {
                    window.removeOnFrameMetricsAvailableListener(listener)
                }
            }
        } else {
            onDispose { }
        }
    }
}
