package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TableDetailResponse(
    @SerializedName("table")
    val table: TableDetailDto? = null,
    @SerializedName("myStats")
    val myStats: TableMyStatsDto? = null
)

data class TableMyStatsDto(
    @SerializedName("playerId") val playerId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("avatarId") val avatarId: String? = null,
    @SerializedName("totalBuyIns") val totalBuyIns: Long = 0L,
    @SerializedName("totalExits") val totalExits: Long = 0L,
    @SerializedName("netBalance") val netBalance: Long = 0L
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
    @SerializedName("playerCount") val playerCount: Int = 0,
    @SerializedName("creator_user_id") val creatorUserId: String? = null,
    @SerializedName("isHostOrAdmin") val isHostOrAdmin: Boolean? = null,
    @SerializedName("canManage") val canManage: Boolean? = null,
    @SerializedName("myStats") val myStats: TableMyStatsDto? = null
)
