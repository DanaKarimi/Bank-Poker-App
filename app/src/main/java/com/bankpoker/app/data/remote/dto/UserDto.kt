package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("created_at")
    val createdAt: Long? = null
)
