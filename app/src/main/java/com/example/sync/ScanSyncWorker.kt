package com.example.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.api.ApiClient
import com.example.api.ScanSyncPayload
import com.example.data.AppDatabase
import com.example.data.SyncStatus

class ScanSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).scanResultDao()
        val pending = dao.getScansBySyncStatus(SyncStatus.PENDING) +
            dao.getScansBySyncStatus(SyncStatus.FAILED)
        if (pending.isEmpty()) return Result.success()

        var failed = false
        pending.forEach { scan ->
            try {
                ApiClient.service.uploadScan(ScanSyncPayload.from(scan))
                dao.updateSyncStatus(scan.id, SyncStatus.SYNCED)
            } catch (_: Exception) {
                dao.updateSyncStatus(scan.id, SyncStatus.FAILED)
                failed = true
            }
        }
        return if (failed) Result.retry() else Result.success()
    }
}

fun enqueueScanSync(context: Context) {
    val request = androidx.work.OneTimeWorkRequestBuilder<ScanSyncWorker>()
        .setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
        )
        .setBackoffCriteria(
            androidx.work.BackoffPolicy.EXPONENTIAL,
            10_000,
            java.util.concurrent.TimeUnit.MILLISECONDS
        )
        .build()
    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
        "mboa-agri-scan-sync",
        androidx.work.ExistingWorkPolicy.KEEP,
        request
    )
}
