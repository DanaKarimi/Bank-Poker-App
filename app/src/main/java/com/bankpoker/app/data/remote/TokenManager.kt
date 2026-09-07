package com.bankpoker.app.data.remote

import android.content.Context
import android.content.SharedPreferences

/**
 * Token manager to securely store and retrieve JWT authentication tokens using SharedPreferences.
 */
class TokenManager(val context: Context) {

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

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun clearToken() {
        prefs.edit().remove(KEY_JWT_TOKEN).apply()
    }

    fun saveUser(username: String, role: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun saveUser(user: com.bankpoker.app.data.remote.dto.UserDto) {
        prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_DISPLAY_NAME, user.displayName ?: user.username)
            .putString(KEY_AVATAR_ID, user.avatarId ?: "avatar_1")
            .putString(KEY_ROLE, user.role)
            .putBoolean(KEY_IS_GUEST, user.isGuest)
            .apply()
    }

    fun getUser(): com.bankpoker.app.data.remote.dto.UserDto? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val displayName = prefs.getString(KEY_DISPLAY_NAME, username)
        val avatarId = prefs.getString(KEY_AVATAR_ID, "avatar_1")
        val role = prefs.getString(KEY_ROLE, "PLAYER") ?: "PLAYER"
        val isGuest = prefs.getBoolean(KEY_IS_GUEST, false)
        return com.bankpoker.app.data.remote.dto.UserDto(
            id = id,
            username = username,
            displayName = displayName,
            avatarId = avatarId,
            role = role,
            isGuest = isGuest
        )
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, getUsername())

    fun getAvatarId(): String = prefs.getString(KEY_AVATAR_ID, "avatar_1") ?: "avatar_1"

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun isGuest(): Boolean = prefs.getBoolean(KEY_IS_GUEST, false)

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "bankpoker_auth_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_DISPLAY_NAME = "auth_display_name"
        private const val KEY_AVATAR_ID = "auth_avatar_id"
        private const val KEY_ROLE = "auth_role"
        private const val KEY_IS_GUEST = "auth_is_guest"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
