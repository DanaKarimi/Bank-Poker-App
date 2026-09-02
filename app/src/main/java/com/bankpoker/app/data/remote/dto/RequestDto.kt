package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RequestDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("group_name")
    val groupName: String? = null,
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("username")
    val username: String? = null,
    @SerializedName("table_id")
    val tableId: String? = null,
    @SerializedName("table_name")
    val tableName: String? = null,
    @SerializedName("amount")
    val amount: Int? = null,
    @SerializedName("status")
    val status: String = "PENDING",
    @SerializedName("created_at")
    val createdAt: Long = 0,
    @SerializedName("updated_at")
    val updatedAt: Long = 0
)
