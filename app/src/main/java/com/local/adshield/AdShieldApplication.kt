package com.local.adshield

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.local.adshield.data.local.AppDatabase
import com.local.adshield.data.worker.BlocklistSyncWorker
import java.util.concurrent.TimeUnit

class AdShieldApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
        schedulePeriodicBlocklistSync()
        triggerInitialSyncIfFirstRun()
    }

    private fun triggerInitialSyncIfFirstRun() {
        val prefs = getSharedPreferences("adshield_app_prefs", MODE_PRIVATE)
        val initialSyncDone = prefs.getBoolean("initial_sync_enqueued", false)
        if (!initialSyncDone) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val initialWork = OneTimeWorkRequestBuilder<BlocklistSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "adshield_initial_sync",
                ExistingWorkPolicy.KEEP,
                initialWork
            )
            prefs.edit().putBoolean("initial_sync_enqueued", true).apply()
        }
    }

    private fun schedulePeriodicBlocklistSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<BlocklistSyncWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "adshield_blocklist_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}
