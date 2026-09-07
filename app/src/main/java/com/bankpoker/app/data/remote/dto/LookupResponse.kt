package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LookupResponse(
    @SerializedName("type")
    val type: String, // "GROUP" or "TABLE"
    @SerializedName("id")
    val id: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("group_name")
    val groupName: String? = null,
    @SerializedName("player_count")
    val playerCount: Int? = null,
    @SerializedName("created_at")
    val createdAt: Any? = null
)
