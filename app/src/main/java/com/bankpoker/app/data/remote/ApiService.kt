package com.bankpoker.app.data.remote

import com.bankpoker.app.data.remote.dto.CreateGroupRequest
import com.bankpoker.app.data.remote.dto.CreateGroupResponse
import com.bankpoker.app.data.remote.dto.CreateTableRequest
import com.bankpoker.app.data.remote.dto.CreateTableResponse
import com.bankpoker.app.data.remote.dto.HealthResponse
import com.bankpoker.app.data.remote.dto.InviteCodeResponse
import com.bankpoker.app.data.remote.dto.LoginRequest
import com.bankpoker.app.data.remote.dto.LoginResponse
import com.bankpoker.app.data.remote.dto.MessageResponse
import com.bankpoker.app.data.remote.dto.PendingRequestsResponse
import com.bankpoker.app.data.remote.dto.RegisterRequest
import com.bankpoker.app.data.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("api/tables/create")
    suspend fun createTable(
        @Body request: CreateTableRequest,
        @Header("Authorization") token: String = ""
    ): Response<CreateTableResponse>

    @GET("api/groups/{id}/invite-code")
    suspend fun getInviteCode(
        @Path("id") groupId: String,
        @Header("Authorization") token: String = ""
    ): Response<InviteCodeResponse>

    @GET("api/requests/pending")
    suspend fun getPendingRequests(
        @Query("groupId") groupId: String,
        @Header("Authorization") token: String = ""
    ): Response<PendingRequestsResponse>

    @POST("api/requests/join/{id}/approve")
    suspend fun approveJoinRequest(
        @Path("id") requestId: String,
        @Header("Authorization") token: String = ""
    ): Response<MessageResponse>

    @POST("api/requests/join/{id}/reject")
    suspend fun rejectJoinRequest(
        @Path("id") requestId: String,
        @Header("Authorization") token: String = ""
    ): Response<MessageResponse>

    @POST("api/requests/buy-in/{id}/approve")
    suspend fun approveBuyInRequest(
        @Path("id") requestId: String,
        @Header("Authorization") token: String = ""
    ): Response<MessageResponse>

    @POST("api/requests/buy-in/{id}/reject")
    suspend fun rejectBuyInRequest(
        @Path("id") requestId: String,
        @Header("Authorization") token: String = ""
    ): Response<MessageResponse>

    @POST("api/requests/exit/{id}/approve")
    suspend fun approveExitRequest(
        @Path("id") requestId: String,
        @Header("Authorization") token: String = ""
    ): Response<MessageResponse>

    @POST("api/requests/exit/{id}/reject")
    suspend fun rejectExitRequest(
        @Path("id") requestId: String,
        @Header("Authorization") token: String = ""
    ): Response<MessageResponse>

    @GET("api/tables/{tableId}/players")
    suspend fun getTablePlayers(
        @Path("tableId") tableId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.TablePlayersResponse>

    @GET("api/tables/{tableId}/buy-ins")
    suspend fun getTableBuyIns(
        @Path("tableId") tableId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.TableBuyInsResponse>

    @GET("api/tables/{tableId}/exits")
    suspend fun getTableExits(
        @Path("tableId") tableId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.TableExitsResponse>

    @GET("api/tables/{tableId}/activity")
    suspend fun getTableActivity(
        @Path("tableId") tableId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.TableActivityResponse>

    @POST("api/tables/{tableId}/buy-in-direct")
    suspend fun directBuyIn(
        @Path("tableId") tableId: String,
        @Body request: com.bankpoker.app.data.remote.dto.DirectBuyInRequest,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.DirectBuyInResponse>

    @POST("api/tables/{tableId}/exit-direct")
    suspend fun directExit(
        @Path("tableId") tableId: String,
        @Body request: com.bankpoker.app.data.remote.dto.DirectExitRequest,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.DirectExitResponse>

    @POST("api/tables/{tableId}/close")
    suspend fun closeTable(
        @Path("tableId") tableId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @GET("api/tables/{tableId}/status")
    suspend fun getTableStatus(
        @Path("tableId") tableId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.google.gson.JsonObject>

    @POST("api/groups/{groupId}/settlement")
    suspend fun syncSettlement(
        @Path("groupId") groupId: String,
        @Body request: com.google.gson.JsonObject,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @POST("api/groups/{groupId}/sync-balances")
    suspend fun syncGroupBalances(
        @Path("groupId") groupId: String,
        @Body request: com.google.gson.JsonObject,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @POST("api/groups/{groupId}/payments")
    suspend fun recordGroupPayment(
        @Path("groupId") groupId: String,
        @Body request: com.google.gson.JsonObject,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @POST("api/groups/import")
    suspend fun importGroup(
        @Body request: com.google.gson.JsonObject,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.ImportGroupResponse>

    @POST("api/groups/{groupId}/invite-code")
    suspend fun syncInviteCode(
        @Path("groupId") groupId: String,
        @Body request: com.google.gson.JsonObject,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @POST("api/tables/{tableId}/entry-fee-sync")
    suspend fun syncEntryFee(
        @Path("tableId") tableId: String,
        @Body request: com.google.gson.JsonObject,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>
}
