package com.bankpoker.app.data.remote

import com.bankpoker.app.data.remote.dto.CreateGroupRequest
import com.bankpoker.app.data.remote.dto.CreateGroupResponse
import com.bankpoker.app.data.remote.dto.HealthResponse
import com.bankpoker.app.data.remote.dto.InviteCodeResponse
import com.bankpoker.app.data.remote.dto.LoginRequest
import com.bankpoker.app.data.remote.dto.LoginResponse
import com.bankpoker.app.data.remote.dto.RegisterRequest
import com.bankpoker.app.data.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

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

    @POST("api/groups/create")
    suspend fun createGroup(
        @Body request: CreateGroupRequest,
        @Header("Authorization") token: String = ""
    ): Response<CreateGroupResponse>

    @GET("api/groups/{id}/invite-code")
    suspend fun getInviteCode(
        @Path("id") groupId: String,
        @Header("Authorization") token: String = ""
    ): Response<InviteCodeResponse>
}
