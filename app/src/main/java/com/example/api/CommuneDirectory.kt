package com.example.api

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Répertoire local des communes officielles du backend MBOA AGRI.
 *
 * Source de vérité : `GET /api/communes?is_active=true` (contrat mobile officiel).
 * Le backend rejette un `commune_code` inconnu en 422 « Code commune inconnu » sur
 * `POST /api/scans` et `POST /api/scans/diagnose`. On met donc la liste en cache et on
 * ne transmet qu'un code réellement connu ; sinon on envoie `null` (champ facultatif).
 *
 * Ce cache est partagé entre l'interface (ViewModel) et le worker de synchronisation,
 * qui n'a pas accès au ViewModel.
 */
object CommuneDirectory {

    private const val PREFS_NAME = "mboa_agri_communes"
    private const val KEY_COMMUNES_JSON = "communes_json"
    private const val KEY_LAST_SYNC = "last_sync_ms"

    /** Recharger la liste au maximum une fois par jour. */
    private const val REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L

    private val moshi: Moshi = Moshi.Builder().build()
    private val listAdapter = moshi.adapter<List<CommuneResponse>>(
        Types.newParameterizedType(List::class.java, CommuneResponse::class.java)
    )

    @Volatile
    private var memoryCache: List<CommuneResponse> = emptyList()

    fun cached(context: Context): List<CommuneResponse> {
        if (memoryCache.isNotEmpty()) return memoryCache
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_COMMUNES_JSON, null) ?: return emptyList()
        val parsed = runCatching { listAdapter.fromJson(raw) }.getOrNull().orEmpty()
        memoryCache = parsed
        return parsed
    }

    fun save(context: Context, communes: List<CommuneResponse>) {
        memoryCache = communes
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COMMUNES_JSON, listAdapter.toJson(communes))
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun lastSyncMs(context: Context): Long =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SYNC, 0L)

    fun needsRefresh(context: Context): Boolean {
        val communes = cached(context)
        if (communes.isEmpty()) return true
        return System.currentTimeMillis() - lastSyncMs(context) > REFRESH_INTERVAL_MS
    }

    /**
     * Télécharge la liste officielle si nécessaire (ou si [force]).
     * Ne lève jamais d'exception : en cas d'échec réseau on conserve le cache.
     */
    suspend fun refresh(context: Context, force: Boolean = false): List<CommuneResponse> {
        if (!force && !needsRefresh(context)) return cached(context)
        return try {
            ApiClient.init(context)
            val page = ApiClient.service.getCommunes(page = 1, pageSize = 100, isActive = true)
            save(context, page.items)
            Log.i("CommuneDirectory", "Liste officielle rechargée : ${page.items.size}/${page.total}")
            page.items
        } catch (e: Exception) {
            Log.w("CommuneDirectory", "Rechargement des communes impossible : ${e.message}")
            cached(context)
        }
    }

    /**
     * Code commune à envoyer au backend.
     *
     * @param communeName nom saisi par l'agriculteur (facultatif)
     * @param explicitCode code sélectionné dans la liste officielle (facultatif)
     * @return un code présent dans la liste officielle, sinon `null` (jamais un code inventé)
     */
    fun resolveCode(
        context: Context,
        communeName: String?,
        explicitCode: String? = null
    ): String? {
        val communes = cached(context)

        val explicit = explicitCode?.trim().orEmpty()
        if (explicit.isNotBlank() && communes.any { it.code.equals(explicit, true) }) {
            return communes.first { it.code.equals(explicit, true) }.code
        }

        val name = communeName?.trim().orEmpty()
        if (name.isNotBlank()) {
            val normalized = normalize(name)
            communes.firstOrNull { normalize(it.name) == normalized }?.let { return it.code }
            communes.firstOrNull { normalize(it.name).contains(normalized) }?.let { return it.code }
        }

        return null
    }

    /** Vrai si aucun code n'est connu localement (cache vide = on ne peut pas valider). */
    fun isCacheEmpty(context: Context): Boolean = cached(context).isEmpty()

    private fun normalize(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase()
            .replace("'", " ")
            .replace("-", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
}
