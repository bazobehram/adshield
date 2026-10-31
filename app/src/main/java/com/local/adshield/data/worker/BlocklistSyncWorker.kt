package com.local.adshield.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.local.adshield.data.repository.BlocklistRepository

class BlocklistSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = BlocklistRepository(applicationContext)
            repository.syncAllEnabledSources()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
