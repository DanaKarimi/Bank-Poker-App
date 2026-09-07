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
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query
import com.bankpoker.app.data.remote.dto.GuestRequest
import com.bankpoker.app.data.remote.dto.ActivateRequest
import com.bankpoker.app.data.remote.dto.UpdateProfileRequest
import com.bankpoker.app.data.remote.dto.LookupResponse
import com.bankpoker.app.data.remote.dto.UserDto

/**
 * Retrofit API Service definition for communication with the Node.js backend.
 */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/guest")
    suspend fun guestJoin(@Body request: GuestRequest): Response<LoginResponse>

    @POST("api/auth/activate")
    suspend fun activateAccount(
        @Body request: ActivateRequest,
        @Header("Authorization") token: String
    ): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<UserDto>

    @PUT("api/auth/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest,
        @Header("Authorization") token: String
    ): Response<UserDto>

    @GET("api/lookup/{code}")
    suspend fun lookupCode(
        @Path("code") code: String,
        @Header("Authorization") token: String = ""
    ): Response<LookupResponse>

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

    @DELETE("api/tables/{tableId}/players/{playerId}")
    suspend fun deleteTablePlayer(
        @Path("tableId") tableId: String,
        @Path("playerId") playerId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @POST("api/tables/quick")
    suspend fun createQuickTable(
        @Body request: com.google.gson.JsonObject,
        @Header("Authorization") token: String = ""
    ): Response<com.google.gson.JsonObject>

    @POST("api/tables/{tableId}/publish")
    suspend fun publishTable(
        @Path("tableId") tableId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.google.gson.JsonObject>

    @GET("api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.NotificationListResponse>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Path("id") id: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @GET("api/notifications/settings")
    suspend fun getNotificationSettings(
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.NotificationSettingsResponse>

    @PUT("api/notifications/settings")
    suspend fun updateNotificationSettings(
        @Body settings: Map<String, Boolean>,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.NotificationSettingsResponse>

    @POST("api/notifications/fcm-token")
    suspend fun registerFcmToken(
        @Body body: Map<String, String>,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    // --- Admin Endpoints ---
    @GET("api/admin/overview")
    suspend fun getAdminOverview(
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.AdminOverviewResponse>

    @GET("api/admin/users")
    suspend fun getAdminUsers(
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.AdminUsersResponse>

    @PUT("api/admin/users/{id}/role")
    suspend fun updateAdminUserRole(
        @Path("id") userId: String,
        @Body body: Map<String, String>,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @DELETE("api/admin/users/{id}")
    suspend fun deleteAdminUser(
        @Path("id") userId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @GET("api/admin/groups")
    suspend fun getAdminGroups(
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.AdminGroupsResponse>

    @DELETE("api/admin/groups/{id}")
    suspend fun deleteAdminGroup(
        @Path("id") groupId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @GET("api/admin/tables")
    suspend fun getAdminTables(
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.AdminTablesResponse>

    @DELETE("api/admin/tables/{id}")
    suspend fun deleteAdminTable(
        @Path("id") tableId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @GET("api/admin/tables/{tableId}/players")
    suspend fun getAdminTablePlayers(
        @Path("tableId") tableId: String,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.AdminTablePlayersResponse>

    @PUT("api/admin/tables/{tableId}/players/{playerId}")
    suspend fun updateAdminTablePlayer(
        @Path("tableId") tableId: String,
        @Path("playerId") playerId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MessageResponse>

    @GET("api/tables/active")
    suspend fun getActiveTables(
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.ActiveTablesResponse>

    @GET("api/groups/my-groups")
    suspend fun getMyGroups(
        @Header("Authorization") token: String = ""
    ): Response<com.bankpoker.app.data.remote.dto.MyGroupsResponse>
}

