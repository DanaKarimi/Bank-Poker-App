package com.bankpoker.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import java.net.URI

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
     * Return the saved Base URL, or default "https://bankjoker.ir" if none is saved.
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
        const val DEFAULT_BASE_URL = "https://bankjoker.ir"

        @Volatile
        private var instance: ServerConfigManager? = null

        fun getInstance(context: Context): ServerConfigManager {
            return instance ?: synchronized(this) {
                instance ?: ServerConfigManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Checks if the given host string is a local or private LAN network target.
         */
        fun isLocalHost(host: String): Boolean {
            val h = host.lowercase().trim()
            if (h == "localhost" || h == "10.0.2.2" || h == "127.0.0.1" || h.endsWith(".local")) {
                return true
            }
            if (h.startsWith("127.") || h.startsWith("10.") || h.startsWith("192.168.")) {
                return true
            }
            // Class B private network: 172.16.0.0 - 172.31.255.255
            if (h.startsWith("172.")) {
                val parts = h.split(".")
                if (parts.size >= 2) {
                    val secondOctet = parts[1].toIntOrNull()
                    if (secondOctet != null && secondOctet in 16..31) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Normalize URL according to requirements:
         * 1. If empty or blank -> return DEFAULT_BASE_URL ("https://bankjoker.ir")
         * 2. Detect scheme or assign default:
         *    - If explicit "https://" or "http://" -> keep it
         *    - If missing: default to "http://" for local LAN hosts, otherwise "https://" for public domains
         * 3. Parse host, port, and path using java.net.URI
         * 4. Port rules:
         *    - If URL has an explicit port (e.g. :8443, :3000) -> preserve it
         *    - If no port specified:
         *        * scheme "https" -> do NOT append :3000 (standard port 443)
         *        * scheme "http" and public domain -> do NOT append :3000 (standard port 80)
         *        * scheme "http" and local host (e.g. 192.168.x.x, localhost) -> append :3000 for legacy LAN dev
         * 5. Scheme and host in lowercase
         * 6. Remove trailing slashes
         */
        fun normalizeUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return DEFAULT_BASE_URL

            val lower = trimmed.lowercase()
            val hasHttps = lower.startsWith("https://")
            val hasHttp = lower.startsWith("http://")

            val urlWithScheme = when {
                hasHttps || hasHttp -> trimmed
                else -> {
                    val candidateHost = trimmed.substringBefore('/').substringBefore(':')
                    val defaultScheme = if (isLocalHost(candidateHost)) "http://" else "https://"
                    "$defaultScheme$trimmed"
                }
            }

            return try {
                val uri = URI(urlWithScheme)
                val scheme = (uri.scheme ?: "https").lowercase()
                val host = uri.host?.lowercase() ?: run {
                    val afterScheme = urlWithScheme.substringAfter("://").substringBefore('/')
                    afterScheme.substringBefore(':').lowercase()
                }
                val explicitPort = uri.port
                val path = (uri.rawPath ?: "").trimEnd('/')

                val finalHostPort = when {
                    explicitPort != -1 -> "$host:$explicitPort"
                    scheme == "https" -> host
                    isLocalHost(host) -> "$host:3000"
                    else -> host
                }

                val result = "$scheme://$finalHostPort$path"
                result.trimEnd('/')
            } catch (e: Exception) {
                fallbackNormalize(urlWithScheme)
            }
        }

        private fun fallbackNormalize(urlWithScheme: String): String {
            val scheme = if (urlWithScheme.startsWith("https://", ignoreCase = true)) "https" else "http"
            val withoutScheme = urlWithScheme.substringAfter("://")
            val hostPortPart = withoutScheme.substringBefore('/')
            val pathPart = if (withoutScheme.contains('/')) "/" + withoutScheme.substringAfter('/') else ""
            val cleanPath = pathPart.trimEnd('/')

            val finalHostPort = if (hostPortPart.contains(':')) {
                hostPortPart
            } else {
                if (scheme == "https" || !isLocalHost(hostPortPart)) {
                    hostPortPart
                } else {
                    "$hostPortPart:3000"
                }
            }
            return "$scheme://$finalHostPort$cleanPath".trimEnd('/')
        }
    }
}
