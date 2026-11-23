package com.example.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SensorRepository
import com.example.data.SensorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs

enum class ToolTab(val title: String, val isProOnly: Boolean) {
    LEVEL("Level", false),
    ANGLE("Angle", true),
    COMPASS("Compass", false),
    METAL("Metal Finder", true)
}

enum class LevelOrientationMode {
    FLAT, VERTICAL
}

class ToolsViewModel(
    sensorRepository: SensorRepository
) : ViewModel() {

    val sensorState: StateFlow<SensorState> = sensorRepository.sensorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SensorState(
                hasAccelerometer = sensorRepository.hasAccelerometer,
                hasMagnetometer = sensorRepository.hasMagnetometer
            )
        )

    private val _selectedTool = MutableStateFlow(ToolTab.LEVEL)
    val selectedTool: StateFlow<ToolTab> = _selectedTool.asStateFlow()

    private val _levelMode = MutableStateFlow(LevelOrientationMode.FLAT)
    val levelMode: StateFlow<LevelOrientationMode> = _levelMode.asStateFlow()

    // Angle Hold state
    private val _isAngleHeld = MutableStateFlow(false)
    val isAngleHeld: StateFlow<Boolean> = _isAngleHeld.asStateFlow()

    private val _heldAngleDeg = MutableStateFlow(0f)
    val heldAngleDeg: StateFlow<Float> = _heldAngleDeg.asStateFlow()

    // Metal Finder Zero/Tare offset in µT
    private val _metalZeroBaselineUt = MutableStateFlow(45f)
    val metalZeroBaselineUt: StateFlow<Float> = _metalZeroBaselineUt.asStateFlow()

    private val _isMetalZeroed = MutableStateFlow(false)
    val isMetalZeroed: StateFlow<Boolean> = _isMetalZeroed.asStateFlow()

    fun selectTool(tool: ToolTab) {
        _selectedTool.value = tool
    }

    fun setLevelOrientationMode(mode: LevelOrientationMode) {
        _levelMode.value = mode
    }

    fun toggleAngleHold(currentAngle: Float) {
        if (_isAngleHeld.value) {
            _isAngleHeld.value = false
        } else {
            _heldAngleDeg.value = currentAngle
            _isAngleHeld.value = true
        }
    }

    fun zeroMetalFinder(currentMagnitudeUt: Float) {
        _metalZeroBaselineUt.value = currentMagnitudeUt
        _isMetalZeroed.value = true
    }

    fun resetMetalZero() {
        _metalZeroBaselineUt.value = 45f
        _isMetalZeroed.value = false
    }

    fun computeNetMagneticSignal(rawUt: Float): Float {
        return abs(rawUt - _metalZeroBaselineUt.value)
    }

    class Factory(private val sensorRepository: SensorRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ToolsViewModel(sensorRepository) as T
        }
    }
}
