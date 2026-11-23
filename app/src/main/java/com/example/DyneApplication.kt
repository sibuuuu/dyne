package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.service.DyneNotificationCenter

class DyneApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 1) Create all notification channels in Application.onCreate with explicit importance
        DyneNotificationCenter.createAllChannels(this)

        // 2) Ensure WorkManager is initialized and sync active notification schedules
        runCatching {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(this, workManagerConfiguration)
            }
        }
        val cachePrefs = getSharedPreferences("dyne_user_prefs_cache", MODE_PRIVATE)
        val isPro = cachePrefs.getBoolean("cached_is_pro", false)
        DyneNotificationCenter.syncAllSchedules(this, isPremiumActive = isPro)

        // 3) Register hardware TorchCallback for Torch widgets & schedule 30-min widget refresh
        com.example.widget.DyneWidgetTorchSynchronizer.ensureRegistered(this)
        com.example.widget.DyneWidgetHelper.schedulePeriodicWidgetRefresh(this)
    }
}
