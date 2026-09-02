package com.bankpoker.app.data.remote

/**
 * Remote API configuration constants and dynamic base URL setting.
 */
object ApiConfig {
    /**
     * Default base URL for Android Emulator pointing to host machine (localhost:3000).
     * For physical Android devices, change to host machine's LAN IP (e.g. "http://192.168.1.100:3000/").
     */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/"
    const val BASE_URL = DEFAULT_BASE_URL

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L

    @Volatile
    var customBaseUrl: String = DEFAULT_BASE_URL
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
        }

    fun getEffectiveBaseUrl(): String = customBaseUrl
}
