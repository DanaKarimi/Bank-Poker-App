package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("avatar_id")
    val avatarId: String? = null,
    @SerializedName("role")
    val role: String = "PLAYER"
)
