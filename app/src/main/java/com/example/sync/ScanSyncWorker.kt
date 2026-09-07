package com.example.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.api.ApiClient
import com.example.api.ScanSyncPayload
import com.example.data.AppDatabase
import com.example.data.SyncStatus
import java.util.concurrent.TimeUnit

/**
 * Worker WorkManager : envoie automatiquement les scans PENDING / FAILED vers
 * le backend dès que le réseau revient.
 *
 *  - `enqueueScanSync`         : déclenché à chaque nouveau scan (ou manuellement).
 *  - `enqueuePeriodicScanSync` : filet de sécurité toutes les 15 minutes pour
 *                                rejouer les envois échoués même sans nouvelle action.
 */
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

private fun networkConstraints(): Constraints =
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

/** Envoi immédiat (dès que le réseau est disponible) après un nouveau scan. */
fun enqueueScanSync(context: Context) {
    val request = OneTimeWorkRequestBuilder<ScanSyncWorker>()
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10_000, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "mboa-agri-scan-sync",
        ExistingWorkPolicy.KEEP,
        request
    )
}

/** Retry périodique des scans non synchronisés (toutes les 15 minutes, réseau requis). */
fun enqueuePeriodicScanSync(context: Context) {
    val request = PeriodicWorkRequestBuilder<ScanSyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10_000, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "mboa-agri-scan-sync-periodic",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}
