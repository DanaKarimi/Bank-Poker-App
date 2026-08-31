package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("user")
    val user: UserDto? = null,
    @SerializedName("error")
    val error: String? = null
)
