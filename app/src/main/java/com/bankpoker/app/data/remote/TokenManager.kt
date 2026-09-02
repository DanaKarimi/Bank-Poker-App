package com.bankpoker.app.data.remote

import android.content.Context
import android.content.SharedPreferences

/**
 * Token manager to securely store and retrieve JWT authentication tokens using SharedPreferences.
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_JWT_TOKEN, null)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_JWT_TOKEN).apply()
    }

    fun saveUser(username: String, role: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "bankpoker_auth_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_ROLE = "auth_role"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
