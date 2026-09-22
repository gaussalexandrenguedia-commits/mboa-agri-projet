package com.example.api

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Gestion du token JWT côté Android.
 * Le token est stocké en SharedPreferences (chiffré si possible en production).
 * Il est utilisé pour authentifier les requêtes vers /api/scans et /api/scans/diagnose.
 *
 * Cycle attendu :
 * inscription -> connexion -> token stocké -> requêtes authentifiées
 */
class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mboa_agri_auth", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_PHONE = "phone_number"
        private const val KEY_COMMUNE_CODE = "commune_code"
        @Volatile private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    fun saveToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    fun saveUserInfo(userId: Int? = null, username: String? = null, phone: String? = null) {
        prefs.edit {
            if (userId != null) putInt(KEY_USER_ID, userId)
            if (username != null) putString(KEY_USERNAME, username)
            if (phone != null) putString(KEY_PHONE, phone)
        }
    }

    fun getUserId(): Int? = if (prefs.contains(KEY_USER_ID)) prefs.getInt(KEY_USER_ID, -1).takeIf { it != -1 } else null
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getPhone(): String? = prefs.getString(KEY_PHONE, null)

    fun saveCommuneCode(code: String) {
        prefs.edit { putString(KEY_COMMUNE_CODE, code) }
    }

    fun getCommuneCode(): String? = prefs.getString(KEY_COMMUNE_CODE, null)

    fun clearAll() {
        prefs.edit { clear() }
    }
}
