package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("avatar_id")
    val avatarId: String? = null,
    @SerializedName("username")
    val username: String? = null
)
