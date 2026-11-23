package com.example.service

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.R
import kotlin.math.max

/**
 * Android Quick Settings TileService for instant Flashlight toggle.
 * Uses CameraManager.turnOnTorchWithStrengthLevel at the middle intensity ("Medium", max(1, max/2))
 * when hardware strength control is supported, or standard setTorchMode fallback.
 */
class FlashlightTileService : TileService() {

    private var cameraManager: CameraManager? = null
    private var torchCameraId: String? = null
    private var maxStrengthLevel: Int = 1
    private var isTorchOn: Boolean = false
    private var currentStrengthLevel: Int = 1

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                isTorchOn = enabled
                syncTileUi()
            }
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            if (cameraId == torchCameraId) {
                currentStrengthLevel = newStrengthLevel
                syncTileUi()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initCameraInfo()
    }

    override fun onStartListening() {
        super.onStartListening()
        initCameraInfo()
        runCatching {
            cameraManager?.registerTorchCallback(torchCallback, null)
        }
        syncTileUi()
    }

    override fun onStopListening() {
        runCatching {
            cameraManager?.unregisterTorchCallback(torchCallback)
        }
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        initCameraInfo()
        val camId = torchCameraId ?: run {
            syncTileUi()
            return
        }
        val nextState = !isTorchOn
        runCatching {
            if (!nextState) {
                cameraManager?.setTorchMode(camId, false)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrengthLevel > 1) {
                val mediumLevel = max(1, maxStrengthLevel / 2)
                cameraManager?.turnOnTorchWithStrengthLevel(camId, mediumLevel)
                currentStrengthLevel = mediumLevel
            } else {
                cameraManager?.setTorchMode(camId, true)
            }
            isTorchOn = nextState
        }
        syncTileUi()
    }

    private fun initCameraInfo() {
        if (cameraManager == null) {
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        }
        if (torchCameraId == null) {
            runCatching {
                cameraManager?.cameraIdList?.forEach { id ->
                    val chars = cameraManager?.getCameraCharacteristics(id) ?: return@forEach
                    val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    if (hasFlash && torchCameraId == null) {
                        torchCameraId = id
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            maxStrengthLevel = (chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1)
                                .coerceAtLeast(1)
                        }
                        currentStrengthLevel = max(1, maxStrengthLevel / 2)
                    }
                }
            }
        }
    }

    private fun syncTileUi() {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_flashlight)
        tile.label = "Dyne Torch"
        if (torchCameraId == null) {
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "No LED"
            }
        } else if (isTorchOn) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (maxStrengthLevel > 1) {
                    val medium = max(1, maxStrengthLevel / 2)
                    when {
                        currentStrengthLevel >= maxStrengthLevel -> "MAX ($currentStrengthLevel/$maxStrengthLevel)"
                        currentStrengthLevel == medium -> "Medium ($currentStrengthLevel/$maxStrengthLevel)"
                        else -> "Lvl $currentStrengthLevel/$maxStrengthLevel"
                    }
                } else {
                    "On"
                }
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Off"
            }
        }
        tile.updateTile()
    }

    companion object {
        fun requestAddQuickSettingsTile(
            context: Context,
            onResult: (String) -> Unit
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                if (statusBarManager != null) {
                    statusBarManager.requestAddTileService(
                        ComponentName(context, FlashlightTileService::class.java),
                        "Dyne Torch",
                        Icon.createWithResource(context, R.drawable.ic_qs_flashlight),
                        context.mainExecutor
                    ) { resultCode ->
                        val msg = when (resultCode) {
                            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                                "Dyne Torch tile added to Quick Settings!"
                            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                                "Dyne Torch tile is already in your Quick Settings."
                            else ->
                                "Quick Settings tile prompt opened."
                        }
                        onResult(msg)
                    }
                    return
                }
            }
            runCatching {
                requestListeningState(context, ComponentName(context, FlashlightTileService::class.java))
            }
            onResult("Dyne Torch tile is registered! Swipe down Quick Settings and tap Edit to place 'Dyne Torch'.")
        }
    }
}
