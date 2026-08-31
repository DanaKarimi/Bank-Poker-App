package com.bankpoker.app.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton API Client providing configured OkHttpClient, Retrofit, and ApiService instances.
 */
object ApiClient {

    private var currentBaseUrl: String = ApiConfig.getEffectiveBaseUrl()
    private var cachedApiService: ApiService? = null

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
     * Get or create the ApiService instance.
     * Automatically handles dynamic base URL updates.
     */
    fun getApiService(
        tokenManager: TokenManager? = null,
        baseUrl: String = ApiConfig.getEffectiveBaseUrl()
    ): ApiService {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        if (cachedApiService == null || currentBaseUrl != normalizedBaseUrl) {
            currentBaseUrl = normalizedBaseUrl
            ApiConfig.customBaseUrl = normalizedBaseUrl

            val okHttpClient = createOkHttpClient(tokenManager)
            val retrofit = createRetrofit(okHttpClient, normalizedBaseUrl)
            cachedApiService = retrofit.create(ApiService::class.java)
        }

        return cachedApiService!!
    }

    /**
     * Reset cached service to force recreation (e.g. after changing Base URL or login)
     */
    fun resetClient() {
        cachedApiService = null
    }
}
