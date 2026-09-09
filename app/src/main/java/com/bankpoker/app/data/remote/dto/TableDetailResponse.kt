package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TableDetailResponse(
    @SerializedName("table")
    val table: TableDetailDto? = null
)

data class TableDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("groupId") val groupId: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String? = null,
    @SerializedName("chipValue") val chipValue: Long? = null,
    @SerializedName("status") val status: String = "ACTIVE",
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("hasEntryFee") val hasEntryFee: Boolean = false,
    @SerializedName("entryFee") val entryFee: Long? = null,
    @SerializedName("createdAt") val createdAt: Long? = null,
    @SerializedName("closedAt") val closedAt: Long? = null,
    @SerializedName("publishedAt") val publishedAt: Long? = null,
    @SerializedName("playerCount") val playerCount: Int = 0
)
