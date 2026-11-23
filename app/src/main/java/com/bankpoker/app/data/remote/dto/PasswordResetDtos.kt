package com.bankpoker.app.data.remote.dto

data class ForgotPasswordRequest(
    val username: String
)

data class ResetPasswordWithCodeRequest(
    val username: String,
    val resetCode: String,
    val newPassword: String
)

data class AdminResetPasswordRequest(
    val newPassword: String? = null,
    val generateCode: Boolean? = null
)

data class AdminResetPasswordResponse(
    val message: String? = null,
    val mode: String? = null,
    val resetCode: String? = null,
    val expiresAt: Long? = null,
    val userId: String? = null,
    val username: String? = null
)

data class PasswordResetRequestDto(
    val id: String,
    val userId: String,
    val username: String,
    val displayName: String? = null,
    val resetCode: String? = null,
    val status: String = "PENDING",
    val expiresAt: Long? = null,
    val createdAt: Long = 0L
)

data class PasswordResetListResponse(
    val requests: List<PasswordResetRequestDto> = emptyList()
)
