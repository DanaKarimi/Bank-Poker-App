package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GuestRequest(
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("avatar_id")
    val avatarId: String? = null
)
