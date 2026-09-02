package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("user")
    val user: UserDto? = null,
    @SerializedName("error")
    val error: String? = null
)
