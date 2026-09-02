package com.bankpoker.app.data.remote

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton API Client providing dynamic, rebuildable OkHttpClient, Retrofit, and ApiService instances.
 * Automatically loads and persists Base URL via ServerConfigManager.
 */
object ApiClient {

    @Volatile
    private var currentBaseUrl: String = ServerConfigManager.DEFAULT_BASE_URL

    @Volatile
    private var cachedRetrofit: Retrofit? = null

    @Volatile
    private var cachedApiService: ApiService? = null

    @Volatile
    private var appContext: Context? = null

    /**
     * Initialize ApiClient with Application context on startup.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        val savedUrl = ServerConfigManager.getInstance(context).getBaseUrl()
        rebuild(context, savedUrl)
    }

    private fun createOkHttpClient(tokenManager: TokenManager?): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val token = tokenManager?.getToken()

            val newRequest = if (!token.isNullOrBlank()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
            chain.proceed(newRequest)
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    private fun createRetrofit(okHttpClient: OkHttpClient, baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Rebuild the Retrofit and ApiService instances with the latest or specified Base URL.
     */
    @Synchronized
    fun rebuild(context: Context? = null, newBaseUrl: String? = null): ApiService {
        val ctx = context?.applicationContext ?: appContext
        val targetUrl = when {
            !newBaseUrl.isNullOrBlank() -> {
                val normalized = ServerConfigManager.normalizeUrl(newBaseUrl)
                if (ctx != null) {
                    ServerConfigManager.getInstance(ctx).saveBaseUrl(normalized)
                }
                normalized
            }
            ctx != null -> ServerConfigManager.getInstance(ctx).getBaseUrl()
            else -> currentBaseUrl
        }

        currentBaseUrl = targetUrl
        ApiConfig.customBaseUrl = targetUrl

        val tokenManager = ctx?.let { TokenManager.getInstance(it) }
        val okHttpClient = createOkHttpClient(tokenManager)
        val retrofit = createRetrofit(okHttpClient, targetUrl)

        cachedRetrofit = retrofit
        cachedApiService = retrofit.create(ApiService::class.java)

        android.util.Log.d("ApiClient", "Retrofit rebuilt successfully with Base URL: $targetUrl")
        return cachedApiService!!
    }

    /**
     * Get or create the ApiService instance.
     */
    fun getApiService(
        context: Context? = null,
        tokenManager: TokenManager? = null
    ): ApiService {
        if (cachedApiService == null) {
            val ctx = context?.applicationContext ?: appContext
            rebuild(ctx)
        }
        return cachedApiService!!
    }

    /**
     * Convenience overload for calls passing only tokenManager.
     */
    fun getApiService(tokenManager: TokenManager?): ApiService {
        if (cachedApiService == null) {
            rebuild(appContext)
        }
        return cachedApiService!!
    }

    /**
     * Get the current active Retrofit instance.
     */
    fun getRetrofit(context: Context? = null): Retrofit {
        if (cachedRetrofit == null) {
            rebuild(context)
        }
        return cachedRetrofit!!
    }

    /**
     * Get the current active Base URL.
     */
    fun getCurrentBaseUrl(context: Context? = null): String {
        val ctx = context?.applicationContext ?: appContext
        return if (ctx != null) {
            ServerConfigManager.getInstance(ctx).getBaseUrl()
        } else {
            currentBaseUrl
        }
    }

    /**
     * Reset cached service to force recreation.
     */
    fun resetClient() {
        cachedApiService = null
        cachedRetrofit = null
    }
}
