package com.bankpoker.app.repository

import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.ApiService
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.data.remote.dto.HealthResponse
import com.bankpoker.app.data.remote.dto.LoginRequest
import com.bankpoker.app.data.remote.dto.LoginResponse
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
