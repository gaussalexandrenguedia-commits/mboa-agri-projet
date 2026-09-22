package com.example.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.BuildConfig

/**
 * Gestion de l'URL de production du backend.
 *
 * - L'URL par défaut provient de BuildConfig.BACKEND_BASE_URL
 *   injectée au build via la variable d'environnement BACKEND_BASE_URL.
 * - Une URL de production fournie par Martial peut être configurée
 *   à l'exécution via les paramètres de l'app (stockée en SharedPreferences).
 * - Le slash final est obligatoire pour Retrofit.
 *
 * Usage build :
 *   export BACKEND_BASE_URL="https://api-mboa-agri.railway.app/"
 *   ./gradlew assembleDebug
 *
 * Usage runtime (après installation) :
 *   Paramètres -> Configuration Backend -> Saisir l'URL de production
 */
object BackendConfig {
    private const val PREFS_NAME = "mboa_agri_backend"
    private const val KEY_CUSTOM_URL = "custom_backend_url"
    private const val KEY_IS_PRODUCTION = "is_production"

    fun getDefaultBaseUrl(): String = BuildConfig.BACKEND_BASE_URL

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getString(KEY_CUSTOM_URL, null)
        return if (!custom.isNullOrBlank()) {
            ensureTrailingSlash(custom.trim())
        } else {
            ensureTrailingSlash(getDefaultBaseUrl())
        }
    }

    fun setCustomBaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleaned = ensureTrailingSlash(url.trim())
        prefs.edit {
            putString(KEY_CUSTOM_URL, cleaned)
            putBoolean(KEY_IS_PRODUCTION, isProductionUrl(cleaned))
        }
    }

    fun clearCustomUrl(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(KEY_CUSTOM_URL)
            remove(KEY_IS_PRODUCTION)
        }
    }

    fun isProduction(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_PRODUCTION, false) || isProductionUrl(getBaseUrl(context))
    }

    fun isProductionUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("https://") && !lower.contains("10.0.2.2") && !lower.contains("localhost") && !lower.contains("127.0.0.1")
    }

    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }

    fun getConfigSummary(context: Context): String {
        val current = getBaseUrl(context)
        val def = getDefaultBaseUrl()
        return if (current == def) {
            "URL active: $current (défaut build)"
        } else {
            "URL active: $current (personnalisée, défaut build: $def)"
        }
    }
}
