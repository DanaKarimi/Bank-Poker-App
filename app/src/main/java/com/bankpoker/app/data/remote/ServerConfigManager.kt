package com.bankpoker.app.data.remote

import android.content.Context
import android.content.SharedPreferences

/**
 * Server Configuration Manager to persist the backend server Base URL
 * across app restarts using SharedPreferences.
 */
class ServerConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Return the saved Base URL, or default "http://10.0.2.2:3000/" if none is saved.
     */
    fun getBaseUrl(): String {
        val saved = prefs.getString(KEY_BASE_URL, null)
        return if (!saved.isNullOrBlank()) {
            normalizeUrl(saved)
        } else {
            DEFAULT_BASE_URL
        }
    }

    /**
     * Save the Base URL after normalizing it.
     */
    fun saveBaseUrl(url: String): String {
        val normalized = normalizeUrl(url)
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        return normalized
    }

    /**
     * Clear the saved Base URL.
     */
    fun clear() {
        prefs.edit().remove(KEY_BASE_URL).apply()
    }

    companion object {
        private const val PREFS_NAME = "bankpoker_server_prefs"
        private const val KEY_BASE_URL = "server_base_url"
        const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/"

        @Volatile
        private var instance: ServerConfigManager? = null

        fun getInstance(context: Context): ServerConfigManager {
            return instance ?: synchronized(this) {
                instance ?: ServerConfigManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Normalize URL according to requirements:
         * 1. Prepend "http://" if missing scheme ("http://" or "https://")
         * 2. Append ":3000" if no port is specified
         * 3. Append "/" if trailing slash is missing
         */
        fun normalizeUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return DEFAULT_BASE_URL

            val scheme = when {
                trimmed.startsWith("https://", ignoreCase = true) -> "https://"
                trimmed.startsWith("http://", ignoreCase = true) -> "http://"
                else -> "http://"
            }

            val withoutScheme = when {
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substring(8)
                trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substring(7)
                else -> trimmed
            }

            val (hostPort, path) = if (withoutScheme.contains("/")) {
                val slashIndex = withoutScheme.indexOf("/")
                Pair(withoutScheme.substring(0, slashIndex), withoutScheme.substring(slashIndex))
            } else {
                Pair(withoutScheme, "")
            }

            val finalHostPort = if (!hostPort.contains(":") && hostPort.isNotBlank()) {
                "$hostPort:3000"
            } else {
                hostPort
            }

            var result = "$scheme$finalHostPort$path"
            if (!result.endsWith("/")) {
                result = "$result/"
            }
            return result
        }
    }
}
