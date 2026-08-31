package com.bankpoker.app.data.remote

import com.bankpoker.app.data.remote.dto.HealthResponse
import com.bankpoker.app.data.remote.dto.LoginRequest
import com.bankpoker.app.data.remote.dto.LoginResponse
import com.bankpoker.app.data.remote.dto.RegisterRequest
import com.bankpoker.app.data.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit API Service definition for communication with the Node.js backend.
 */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("api/health")
    suspend fun healthCheck(): Response<HealthResponse>
}
