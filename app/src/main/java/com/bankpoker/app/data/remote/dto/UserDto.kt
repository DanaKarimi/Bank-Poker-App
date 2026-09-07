package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("avatar_id")
    val avatarId: String? = null,
    @SerializedName("role")
    val role: String = "PLAYER",
    @SerializedName("is_guest")
    val isGuest: Boolean = false,
    @SerializedName("created_at")
    val createdAt: Any? = null
)
