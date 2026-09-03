package com.bankpoker.app.data.remote

/**
 * Remote API configuration constants and dynamic base URL setting.
 */
object ApiConfig {
    /**
     * Default production base URL.
     * Can be customized at runtime via ServerConfigManager or ApiClient.rebuild.
     */
    const val DEFAULT_BASE_URL = "https://bankjoker.ir/"
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
