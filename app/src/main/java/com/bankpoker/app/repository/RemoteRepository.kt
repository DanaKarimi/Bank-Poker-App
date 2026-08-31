package com.bankpoker.app.repository

import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.ApiService
import com.bankpoker.app.data.remote.TokenManager
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
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository responsible for all network operations against the Node.js backend server.
 */
class RemoteRepository(
    private var apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun updateApiService(newService: ApiService) {
        this.apiService = newService
    }

    private fun getAuthHeader(): String {
        val token = tokenManager.getToken()
        return if (!token.isNullOrBlank()) "Bearer $token" else ""
    }

    /**
     * Test connection against the server /api/health endpoint
     */
    suspend fun healthCheck(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.healthCheck()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Health check failed (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot reach server. Please check your Base URL and WiFi connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Log in a user and save the JWT token
     */
    suspend fun login(username: String, password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(username.trim(), password)
            val response = apiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                body.token?.let { token ->
                    tokenManager.saveToken(token)
                }
                body.user?.let { user ->
                    tokenManager.saveUser(user.username, user.role)
                }
                Result.success(body)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Login failed (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register a new user
     */
    suspend fun register(
        username: String,
        password: String,
        role: String = "PLAYER"
    ): Result<RegisterResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(username.trim(), password, role)
            val response = apiService.register(request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Registration failed (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new remote group on the server (Admin only)
     */
    suspend fun createGroup(name: String, mode: String = "OFFLINE"): Result<CreateGroupResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateGroupRequest(name.trim(), mode)
            val response = apiService.createGroup(request, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Create group failed (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new remote table on the server (Admin only)
     */
    suspend fun createTable(
        groupId: String,
        name: String,
        chipValue: Long? = null,
        entryFee: Long? = null
    ): Result<CreateTableResponse> = withContext(Dispatchers.IO) {
        try {
            val request = com.bankpoker.app.data.remote.dto.CreateTableRequest(
                groupId = groupId,
                name = name.trim(),
                chipValue = chipValue,
                entryFee = entryFee
            )
            val response = apiService.createTable(request, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Create table failed (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieve invite code for a group (Admin only)
     */
    suspend fun getInviteCode(groupId: String): Result<InviteCodeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getInviteCode(groupId, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Failed to get invite code (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get pending requests for an online group (Admin only)
     */
    suspend fun getPendingRequests(groupId: String): Result<PendingRequestsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPendingRequests(groupId, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Failed to fetch pending requests (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Approve a join request (Admin only)
     */
    suspend fun approveJoinRequest(requestId: String): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.approveJoinRequest(requestId, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Failed to approve join request (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reject a join request (Admin only)
     */
    suspend fun rejectJoinRequest(requestId: String): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.rejectJoinRequest(requestId, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Failed to reject join request (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Approve a buy-in request (Admin only)
     */
    suspend fun approveBuyInRequest(requestId: String): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.approveBuyInRequest(requestId, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Failed to approve buy-in request (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reject a buy-in request (Admin only)
     */
    suspend fun rejectBuyInRequest(requestId: String): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.rejectBuyInRequest(requestId, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Failed to reject buy-in request (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Approve an exit request (Admin only)
     */
    suspend fun approveExitRequest(requestId: String): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.approveExitRequest(requestId, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Failed to approve exit request (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reject an exit request (Admin only)
     */
    suspend fun rejectExitRequest(requestId: String): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.rejectExitRequest(requestId, getAuthHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Failed to reject exit request (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Cannot connect to server."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = gson.fromJson(errorBody, JsonObject::class.java)
            if (json.has("error")) json.get("error").asString
            else if (json.has("message")) json.get("message").asString
            else errorBody
        } catch (e: Exception) {
            errorBody
        }
    }
}
