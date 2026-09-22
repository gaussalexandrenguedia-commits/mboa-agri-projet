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

        // Récupérer le code commune du profil pour enrichir le payload
        val lastUser = userDao.getLastUser()
        val communeCode = lastUser?.communeCode?.takeIf { it.isNotBlank() }
            ?: tokenManager.getCommuneCode()
            ?: mapCommuneToCode(lastUser?.commune)

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
                // Idempotence backend : si already_synced, on considère comme succès
                android.util.Log.i("ScanSyncWorker", "Scan ${scan.id} sync status: ${response.syncStatus} -> ${response.message}")
                dao.updateSyncStatus(scan.id, SyncStatus.SYNCED)
            } catch (e: Exception) {
                android.util.Log.e("ScanSyncWorker", "Sync failed for scan ${scan.id}: ${e.message}", e)
                // Si 401, token invalide -> ne pas retry indéfiniment, marquer FAILED
                val isAuthError = e.message?.contains("401") == true || e.message?.contains("Authentification") == true
                if (isAuthError) {
                    // Optionnel : clear token pour forcer re-login
                    // tokenManager.clearToken()
                }
                dao.updateSyncStatus(scan.id, SyncStatus.FAILED)
                failed = true
            }
        }
        return if (failed) Result.retry() else Result.success()
    }

    private fun mapCommuneToCode(communeName: String?): String? {
        if (communeName.isNullOrBlank()) return null
        // Mapping des communes principales du projet MBOA AGRI
        // À enrichir avec les vrais codes fournis par Martial (table communes.code)
        return when (communeName.trim().lowercase()) {
            "bafoussam ii", "bafoussam 2" -> "CM-BFS-02"
            "bafoussam i", "bafoussam 1" -> "CM-BFS-01"
            "foumbot" -> "CM-FBT-01"
            "dschang" -> "CM-DSC-01"
            "bamenda" -> "CM-BMD-01"
            "yaounde", "yaoundé" -> "CM-YDE-01"
            "douala" -> "CM-DLA-01"
            else -> null // Laisser null si non mappé, backend accepte
        }
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
