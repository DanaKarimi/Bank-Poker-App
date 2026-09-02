package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroupDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("invite_code")
    val inviteCode: String? = null,
    @SerializedName("mode")
    val mode: String? = "OFFLINE",
    @SerializedName("created_by")
    val createdBy: String? = null,
    @SerializedName("created_at")
    val createdAt: Long? = null
)
