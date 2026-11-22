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
import com.example.api.CommuneDirectory
import com.example.api.ScanSyncPayload
import com.example.api.TokenManager
import com.example.data.AppDatabase
import com.example.data.SyncStatus
import java.util.concurrent.TimeUnit

/**
 * Worker WorkManager : envoie automatiquement les scans PENDING / FAILED vers
 * le backend dès que le réseau revient.
 *
 * Flux complet testé :
 * 1. Scan créé localement -> PENDING (Room)
 * 2. Réseau disponible -> Worker déclenché
 * 3. JWT récupéré depuis TokenManager
 * 4. POST /api/scans avec Authorization Bearer
 * 5. Succès -> SYNCED, sinon FAILED + retry exponentiel
 * 6. Vérifiable dans PostgreSQL (table scans + diagnostics)
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
        val context = applicationContext
        ApiClient.init(context)
        ApiClient.rebuild()

        val db = AppDatabase.getDatabase(context)
        val dao = db.scanResultDao()
        val userDao = db.userDao()
        val pending = dao.getScansBySyncStatus(SyncStatus.PENDING) +
            dao.getScansBySyncStatus(SyncStatus.FAILED)
        if (pending.isEmpty()) return Result.success()

        val tokenManager = TokenManager.getInstance(context)
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            // Pas de token JWT : on ne peut pas synchroniser, on retry plus tard après login
            android.util.Log.w("ScanSyncWorker", "Aucun token JWT, synchronisation reportée")
            return Result.retry()
        }

        // Code commune : uniquement un code officiel (GET /api/communes).
        // Un code inventé provoque un 422 côté backend et bloque la synchronisation.
        val lastUser = userDao.getLastUser()
        CommuneDirectory.refresh(applicationContext, force = false)
        val communeCode = CommuneDirectory.resolveCode(
            context = applicationContext,
            communeName = lastUser?.commune,
            explicitCode = lastUser?.communeCode?.takeIf { it.isNotBlank() }
                ?: tokenManager.getCommuneCode()
        )
        if (communeCode == null && lastUser?.communeCode?.isNotBlank() == true) {
            android.util.Log.w(
                "ScanSyncWorker",
                "commune_code '${lastUser.communeCode}' absent de la liste officielle : envoyé à null"
            )
        }

        var failed = false
        pending.forEach { scan ->
            try {
                val payload = ScanSyncPayload(
                    localId = scan.id,
                    plantName = scan.plantName,
                    diseaseName = scan.diseaseName,
                    confidence = scan.confidence,
                    symptoms = scan.symptoms,
                    treatmentLocal = scan.treatmentLocal,
                    treatmentChemical = scan.treatmentChemical,
                    timestamp = scan.timestamp,
                    latitude = scan.latitude,
                    longitude = scan.longitude,
                    horsLigne = true,
                    communeCode = communeCode
                )
                val response = ApiClient.service.uploadScan(payload)
                // Idempotence backend : 200 + already_synced signifie que le serveur possède déjà
                // ce local_id ; on marque le scan comme synchronisé localement (contrat §3.1).
                android.util.Log.i("ScanSyncWorker", "Scan ${scan.id} sync status: ${response.syncStatus} -> ${response.message}")
                dao.updateSyncStatus(scan.id, SyncStatus.SYNCED)
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                android.util.Log.e("ScanSyncWorker", "Sync HTTP $code pour le scan ${scan.id} : $body", e)
                dao.updateSyncStatus(scan.id, SyncStatus.FAILED)
                when (code) {
                    // 401/403 : token absent, invalide ou compte désactivé -> attendre une reconnexion
                    401, 403 -> {
                        android.util.Log.w("ScanSyncWorker", "JWT refusé : reconnexion nécessaire")
                        return Result.retry()
                    }
                    // 422 : payload refusé (ex. code commune inconnu) -> ne sert à rien de rejouer
                    413, 415, 422 -> android.util.Log.w(
                        "ScanSyncWorker",
                        "Scan ${scan.id} refusé par le backend (données invalides), nouveau scan nécessaire"
                    )
                    else -> failed = true
                }
            } catch (e: Exception) {
                android.util.Log.e("ScanSyncWorker", "Sync failed for scan ${scan.id}: ${e.message}", e)
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
